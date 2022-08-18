package org.cirruslabs.sq.github.hooks

import org.cirruslabs.sq.github.api.Repository

// https://docs.github.com/en/developers/webhooks-and-events/webhooks/webhook-events-and-payloads#issue_comment

data class IssueCommentedEvent(
  override val action: String,
  override val installation: Installation,
  val issue: Issue,
  val comment: Comment,
  val repository: Repository
) : Event

data class Issue(val number: Int)
data class Comment(val body: String)
