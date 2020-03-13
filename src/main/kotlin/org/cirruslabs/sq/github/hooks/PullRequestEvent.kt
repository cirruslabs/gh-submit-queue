package org.cirruslabs.sq.github.hooks

import org.cirruslabs.sq.github.api.PullRequest
import org.cirruslabs.sq.github.api.Repository

// https://developer.github.com/v3/activity/events/types/#pullrequestevent

data class PullRequestEvent(
  override val action: String,
  override val installation: Installation,
  val number: Long,
  val repository: Repository,
  val pull_request: PullRequest
) : Event
