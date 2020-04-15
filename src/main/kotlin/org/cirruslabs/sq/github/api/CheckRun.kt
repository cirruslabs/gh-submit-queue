package org.cirruslabs.sq.github.api


data class CheckRun(
  val id: Long,
  val name: String,
  val head_sha: String,
  val html_url: String,
  val status: CheckRunStatus?,
  val conclusion: CheckRunConclusion?,
  val app: App
) {
  val successful: Boolean
    get() = status == CheckRunStatus.completed &&
      (conclusion == CheckRunConclusion.neutral || conclusion == CheckRunConclusion.success)
}

enum class CheckRunStatus {
  queued, in_progress, completed
}

enum class CheckRunConclusion {
  success, failure, neutral, timed_out, cancelled, action_required
}
