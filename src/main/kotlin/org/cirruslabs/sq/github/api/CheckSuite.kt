package org.cirruslabs.sq.github.api


data class CheckSuite(
  val id: Long,
  val head_branch: String,
  val head_sha: String,
  val check_runs_url: String,
  val status: CheckSuiteStatus?,
  val conclusion: CheckSuiteConclusion?,
  val app: App
) {
  // GH always creates a check suite even if an App don't react to it
  // For example WIP app doesn't create a check suite for master branch but API return it
  val notInitialized: Boolean
    get() = status == null

  val successful: Boolean
    get() = status == CheckSuiteStatus.completed &&
      (conclusion == CheckSuiteConclusion.neutral || conclusion == CheckSuiteConclusion.success)
}

enum class CheckSuiteStatus {
  requested, in_progress, completed
}

enum class CheckSuiteConclusion {
  success, failure, neutral, cancelled, timed_out, action_required, stale
}
