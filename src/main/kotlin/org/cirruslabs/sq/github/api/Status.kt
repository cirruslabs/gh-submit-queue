package org.cirruslabs.sq.github.api

data class Status(
  val state: StatusState,
  val description: String,
  val target_url: String = "",
  val context: String = "submit-queue"
)

enum class StatusState {
  error, failure, pending, success
}
