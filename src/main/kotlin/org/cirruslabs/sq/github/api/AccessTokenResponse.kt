package org.cirruslabs.sq.github.api

import java.time.Duration
import java.util.*

data class AccessTokenResponse(
  var token: String,
  var expires_at: Date
) {
  val expiresIn: Duration
    get() = Duration.between(Date().toInstant(), expires_at.toInstant())
}
