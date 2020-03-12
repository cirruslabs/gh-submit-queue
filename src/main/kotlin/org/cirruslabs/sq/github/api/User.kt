package org.cirruslabs.sq.github.api


data class User(
  val id: Long,
  val login: String,
  val type: UserType
)

enum class UserType {
  User, Organization
}
