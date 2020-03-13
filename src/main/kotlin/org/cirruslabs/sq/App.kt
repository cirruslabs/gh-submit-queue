package org.cirruslabs.sq

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.DoubleReceive
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.time.withTimeout
import org.cirruslabs.sq.github.hooks.CheckSuiteEvent
import org.cirruslabs.sq.github.hooks.PullRequestEvent
import org.cirruslabs.sq.github.impl.GitHubAPIImpl
import org.cirruslabs.sq.github.impl.GitHubAccessTokenManagerImpl
import org.cirruslabs.sq.github.impl.GithubAppSecretsImpl
import org.cirruslabs.sq.utils.constantTimeEquals
import java.time.Duration

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
fun main() {
  val githubSecrets = GithubAppSecretsImpl.initialize()
  val gitHubScheme = System.getenv().getOrDefault("GITHUB_API_SCHEME", "https")
  val gitHubHost = System.getenv().getOrDefault("GITHUB_API_HOST", "api.github.com")
  val httpClient = HttpClient(CIO) {
    install(JsonFeature)
  }
  val tokenManager = GitHubAccessTokenManagerImpl(
    baseAPIScheme = gitHubScheme,
    baseAPIHost = gitHubHost,
    secrets = githubSecrets,
    httpClient = httpClient
  )
  val api = GitHubAPIImpl(tokenManager, gitHubScheme, gitHubHost, httpClient)
  val logic = SubmitQueueLogic(api)

  val portToListen = System.getenv().getOrDefault("PORT", "8080").toInt()
  val server = embeddedServer(Netty, port = portToListen) {
    install(DefaultHeaders)
    install(DoubleReceive) {
      // because we need to double receive in case of signature verification
      receiveEntireContent = true
    }
    install(ContentNegotiation) {
      gson {
        setPrettyPrinting()
      }
    }
    install(StatusPages) {
      exception<Throwable> { cause ->
        // log errors
        cause.printStackTrace()
        call.respond(HttpStatusCode.InternalServerError)
      }
    }
    routing {
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
        if (signature != null) {
          val expectedSignature = githubSecrets.calculateSignature(call.receive())
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
  }
  server.start(wait = true)
}

private suspend fun processEvent(logic: SubmitQueueLogic, event: String, deliveryId: String, call: ApplicationCall): Unit {
  when (event) {
    "check_suite" -> {
      val eventPayload = call.receive<CheckSuiteEvent>()
      val owner = eventPayload.repository.owner.login
      val name = eventPayload.repository.name

      println("Processing check suite ${eventPayload.check_suite.id} for $owner/$name repository...")
      logic.checkReference(eventPayload.installation.id, owner, name, eventPayload.check_suite.head_branch)
      call.respondText("Processed!")
    }
    "pull_request" -> {
      val eventPayload = call.receive<PullRequestEvent>()
      val owner = eventPayload.repository.owner.login
      val name = eventPayload.repository.name
      val ref = eventPayload.pull_request.base.ref
      val sha = eventPayload.pull_request.head.sha

      if (eventPayload.action == "opened") {
        println("Processing creation of PR #${eventPayload.number} for $owner/$name repository...")
        logic.checkReferenceAndSetForSHA(eventPayload.installation.id, owner, name, ref, sha)
      } else {
        println("No need to process action ${eventPayload.action} of PR #${eventPayload.number} for $owner/$name repository...")
      }
      call.respondText("Processed!")
    }
    else -> {
      val message = "Event '$event' of $deliveryId delivery is not supported!"
      println(message)
      call.respondText(message)
    }
  }
}
