package org.cirruslabs.sq.github

interface GitHubAccessTokenManager {
  suspend fun acquireAccessToken(installationId: Long): String?
}
