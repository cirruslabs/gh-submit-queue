package org.cirruslabs.sq.github.api


data class Repository(
  val name: String,
  val owner: User,
  val default_branch: String
)
