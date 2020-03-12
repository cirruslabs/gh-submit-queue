package org.cirruslabs.sq.github

interface GithubAppSecrets {
  val appId: Long
  val clientId: String
  fun signJWT(): String
  fun calculateSignature(data: ByteArray): String
}
