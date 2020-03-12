package org.cirruslabs.sq.github.api

data class CheckSuitesResponse(
  val total_count: Long,
  val check_suites: List<CheckSuite>
)
