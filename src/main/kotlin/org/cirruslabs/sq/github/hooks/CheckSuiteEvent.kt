package org.cirruslabs.sq.github.hooks

import org.cirruslabs.sq.github.api.CheckSuite
import org.cirruslabs.sq.github.api.Repository

// https://developer.github.com/v3/activity/events/types/#checksuiteevent

data class CheckSuiteEvent(
  override val action: String,
  override val installation: Installation,
  val check_suite: CheckSuite,
  val repository: Repository
) : Event
