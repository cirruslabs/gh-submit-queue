package org.cirruslabs.sq

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.time.withTimeout
import org.cirruslabs.sq.github.GitHubAPI
import org.cirruslabs.sq.github.GithubAppSecrets
import org.cirruslabs.sq.github.hooks.CheckSuiteEvent
import org.cirruslabs.sq.github.hooks.IssueCommentedEvent
import org.cirruslabs.sq.github.hooks.PullRequestEvent
import org.cirruslabs.sq.utils.constantTimeEquals
import java.time.Duration

class SubmitQueueApplication(
  private val api: GitHubAPI,
  private val secrets: GithubAppSecrets? = null
) {
  fun setup(routing: Routing) = routing.apply {
    val logic = SubmitQueueLogic(api)

    get("/") {
      call.respondText("There is no UI for the GitHub App!")
    }
    post("/hooks/github") {
      val deliveryId = call.request.header("X-GitHub-Delivery")?.toLowerCase()
        ?: throw IllegalStateException("No delivery id!")
      val event = call.request.header("X-GitHub-Event")?.toLowerCase()
        ?: throw IllegalStateException("No event name!")

      println("Receiving $deliveryId delivery for $event event...")

      val signature = call.request.header("X-Hub-Signature")?.toLowerCase()
      if (signature != null && secrets != null) {
        val expectedSignature = secrets.calculateSignature(call.receive())
        if (!expectedSignature.constantTimeEquals(signature)) {
          System.err.println("Failed to verify signature! $expectedSignature != $signature")
          throw IllegalStateException("Wrong signature!")
        } else {
          println("Expected signature for $deliveryId delivery!")
        }
      }

      try {
        // GH has a 10 seconds timeout for the delivering. Reserve 1 second for network and stuff.
        withTimeout(Duration.ofSeconds(9)) {
          processEvent(logic, event, deliveryId, call)
        }
      } catch (e: TimeoutCancellationException) {
        // this is not too bad ¯\_(ツ)_/¯
        // can happen because of the large amount of PRs to update
        // but the a lot of very recent PRs should be updated within the timeout
        // and we are running as a lambda so we can't have a state like a worker queue to update PRs async
        call.respondText("Timed out!")
      }
    }
  }

  private suspend fun processEvent(
    logic: SubmitQueueLogic,
    event: String,
    deliveryId: String,
    call: ApplicationCall
  ): Unit {
    when (event) {
      "check_suite" -> {
        val eventPayload = call.receive<CheckSuiteEvent>()
        val owner = eventPayload.repository.owner.login
        val name = eventPayload.repository.name

        println("Processing check suite ${eventPayload.check_suite.id} for $owner/$name repository...")
        val branch = eventPayload.check_suite.head_branch ?: eventPayload.repository.default_branch
        logic.checkReference(eventPayload.installation.id, owner, name, branch)
        call.respondText("Processed!")
      }

      "pull_request" -> {
        val eventPayload = call.receive<PullRequestEvent>()
        val owner = eventPayload.repository.owner.login
        val name = eventPayload.repository.name
        val ref = eventPayload.pull_request.base.ref
        val sha = eventPayload.pull_request.head.sha

        if (eventPayload.action == "opened" || eventPayload.action == "reopened") {
          println("Processing creation of PR #${eventPayload.number} for $owner/$name repository...")
          logic.checkReferenceAndSetForSHA(eventPayload.installation.id, owner, name, ref, sha)
        } else {
          println("No need to process action ${eventPayload.action} of PR #${eventPayload.number} for $owner/$name repository...")
        }
        call.respondText("Processed!")
      }

      "issue_commented" -> {
        val eventPayload = call.receive<IssueCommentedEvent>()
        val installationId = eventPayload.installation.id
        val owner = eventPayload.repository.owner.login
        val name = eventPayload.repository.name
        val prNumber = eventPayload.issue.number

        if (eventPayload.comment.body.contains("/sq poke")) {
          val pullRequest = api.prInfo(installationId, owner, name, prNumber) ?: return call.respondText("Processed!")
          val ref = pullRequest.base.ref
          val sha = pullRequest.head.sha
          logic.checkReferenceAndSetForSHA(installationId, owner, name, ref, sha)
        }
      }

      else -> {
        val message = "Event '$event' of $deliveryId delivery is not supported!"
        println(message)
        call.respondText(message)
      }
    }
  }
}
