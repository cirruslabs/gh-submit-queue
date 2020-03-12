package org.cirruslabs.sq

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
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
    val checkSuites = api.listCheckSuites(installationId, owner, repo, ref)
    val overallConclusion = overallConclusion(checkSuites)
    if (!overallConclusion.allCompleted && overallConclusion.failureDetails == null) {
      println("Seems checks are still running for $owner/$repo@$ref");
      return;
    }
    val params = mapOf(
      "base" to ref,
      "state" to "open",
      "sort" to "updated",
      "direction" to "desc"
    )
    // todo: offload to a channel
    val status = commitStatusFromConclusion(overallConclusion)
    api.listPullRequests(installationId, owner, repo, params).collect { pr ->
      println("Updating PR #${pr.number} of $owner/$repo to ${status.state} state...")
      api.setStatus(installationId, owner, repo, pr.head.sha, status)
    }
  }

  private fun commitStatusFromConclusion(conclusion: Conclusion): Status {
    val defaultStatus = Status(
      context = "submit-queue",
      description = "Checks are running...",
      state = StatusState.pending
    );
    if (conclusion.allCompleted) {
      return if (conclusion.failureDetails != null) {
        defaultStatus.copy(
            state = StatusState.failure,
            description = "${conclusion.failureDetails.appName} ${conclusion.failureDetails.status}",
            target_url = conclusion.failureDetails.url
        )
      } else {
        defaultStatus.copy(
            state = StatusState.success,
            description = "All checks are passing!"
        )
      }
    }
    return defaultStatus;
  }

  private suspend fun overallConclusion(checkSuitesFlow: Flow<CheckSuite>): Conclusion {
    // collect all check suites since we need to iterate twice
    val checkSuites = LinkedList<CheckSuite>()
    checkSuitesFlow.collect { checkSuite ->
      checkSuites.add(checkSuite)
    }
    // first check if there is any suite that completed but not in a successful state to report it ASAP
    checkSuites.firstOrNull { check -> check.notSuccessful }?.also { failedCheck ->
      return Conclusion(
        allCompleted = false,
        failureDetails = ConclusionDetails(failedCheck.app.name, failedCheck.check_runs_url, failedCheck.conclusion?.name
          ?: "failed")
      )
    }

    // there are no failing checks at the moment!
    // let's check if any of them are still running e.g. no in completed state
    for (checkSuite in checkSuites) {
      if (checkSuite.status != CheckSuiteStatus.completed) {
        return Conclusion(allCompleted = false);
      }
    }
    return Conclusion(allCompleted = true)
  }
}