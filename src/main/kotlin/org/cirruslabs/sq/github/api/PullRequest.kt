package org.cirruslabs.sq.github.api


data class PullRequest(
  val number: Long,
  val state: PullRequestState,
  val locked: Boolean,
  val title: String,
  val body: String,
  val user: User,
  val labels: List<IssueLabel>,
  val head: PullRequestRepositoryReference,
  val base: PullRequestRepositoryReference
)

enum class PullRequestState {
  open, closed
}


data class PullRequestRepositoryReference(
  val ref: String,
  val sha: String,
  val repo: Repository
)
