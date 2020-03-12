package org.cirruslabs.sq.github.hooks

interface Event {
  val action: String
  val installation: Installation
}
