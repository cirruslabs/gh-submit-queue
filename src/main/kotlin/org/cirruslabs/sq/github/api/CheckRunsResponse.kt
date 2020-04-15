package org.cirruslabs.sq.github.api

data class CheckRunsResponse(
  val total_count: Long,
  val check_runs: List<CheckRun>
)
