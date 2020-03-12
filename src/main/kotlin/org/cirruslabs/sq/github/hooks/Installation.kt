package org.cirruslabs.sq.github.hooks

import org.cirruslabs.sq.github.api.User


data class Installation(
  val id: Long,
  val app_id: Long,
  val target_id: Long,
  val target_type: String,
  val account: User
)
