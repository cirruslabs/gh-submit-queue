package org.cirruslabs.sq

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import org.cirruslabs.sq.github.GitHubAPI
import org.cirruslabs.sq.github.api.CheckSuite
import org.cirruslabs.sq.github.api.CheckSuiteStatus
import org.cirruslabs.sq.github.api.Status
import org.cirruslabs.sq.github.api.StatusState
import org.cirruslabs.sq.model.Conclusion
import org.cirruslabs.sq.model.ConclusionDetails
import java.util.*

class SubmitQueueLogic(val api: GitHubAPI) {
  /**
   * @param owner is a login of a user or an organization
   * @param repo is a repository name
   * @param ref can be a SHA, branch name, or a tag name
   */
  suspend fun checkReference(installationId: Long, owner: String, repo: String, ref: String) {
    val overallConclusion = overallConclusion(installationId, owner, repo, ref)
    if (!overallConclusion.completed && overallConclusion.failureDetails == null) {
      println("Seems checks are still running for $owner/$repo@$ref")
      return
    }
    val params = mapOf(
      "base" to ref,
      "state" to "open",
      "sort" to "updated",
      "direction" to "desc"
    )
    val status = commitStatusFromConclusion(overallConclusion, ref)
    println("Checks completed for $owner/$repo@$ref in ${status.state} state!")
    // make it parallel once https://github.com/Kotlin/kotlinx.coroutines/issues/1147 is released
    api.listPullRequests(installationId, owner, repo, params).collect { pr ->
      println("Updating PR #${pr.number} of $owner/$repo to ${status.state} state...")
      try {
        api.setStatus(installationId, owner, repo, pr.head.sha, status)
      } catch (ex: Throwable) {
        ex.printStackTrace()
      }
    }
  }

  private fun commitStatusFromConclusion(conclusion: Conclusion, ref: String): Status {
    val defaultStatus = Status(
      context = "submit-queue",
      description = "Checks are running...",
      state = StatusState.pending
    );
    if (conclusion.completed) {
      return if (conclusion.failureDetails != null) {
        defaultStatus.copy(
          state = StatusState.failure,
          description = "${conclusion.failureDetails.appName} ${conclusion.failureDetails.status} on $ref",
          target_url = conclusion.failureDetails.url
        )
      } else {
        defaultStatus.copy(
          state = StatusState.success,
          description = "Ready to merge!"
        )
      }
    }
    return defaultStatus;
  }

  suspend fun overallConclusion(installationId: Long, owner: String, repo: String, ref: String): Conclusion {
    val checkSuitesFlow = api.listCheckSuites(installationId, owner, repo, ref)
    // collect all check suites since we need to iterate twice
    val checkSuites = LinkedList<CheckSuite>()
    checkSuitesFlow.filterNot { it.notInitialized }.collect { checkSuite ->
      checkSuites.add(checkSuite)
    }
    println("Found ${checkSuites.size} check suites: $checkSuites")
    // first check if there is any suite that completed but not in a successful state to report it ASAP
    checkSuites.firstOrNull { check -> !check.successful }?.also { failedCheck ->
      val failedRun = try {
        api.listCheckRuns(installationId, owner, repo, failedCheck.id).first { !it.successful }
      } catch (ex: NoSuchElementException) {
        null
      }
      return Conclusion(
        completed = true,
        failureDetails = ConclusionDetails(
          failedCheck.app.name,
          failedRun?.html_url ?: failedCheck.check_runs_url,
          failedCheck.conclusion?.name ?: "failed"
        )
      )
    }

    // there are no failing checks at the moment!
    // let's check if any of them are still running e.g. no in completed state
    for (checkSuite in checkSuites) {
      if (checkSuite.status != CheckSuiteStatus.completed) {
        return Conclusion(completed = false);
      }
    }
    return Conclusion(completed = true)
  }

  suspend fun checkReferenceAndSetForSHA(installationId: Long, owner: String, repo: String, ref: String, sha: String) {
    val overallConclusion = overallConclusion(installationId, owner, repo, ref)
    val status = commitStatusFromConclusion(overallConclusion, ref)
    println("Checks completed for $owner/$repo@$ref in ${status.state} state!")
    try {
      api.setStatus(installationId, owner, repo, sha, status)
    } catch (ex: Throwable) {
      ex.printStackTrace()
    }
  }
}
