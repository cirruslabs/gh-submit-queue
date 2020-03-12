package org.cirruslabs.sq.github.impl

import org.cirruslabs.sq.github.GitHubAccessTokenManager

object TestGitHubAccessTokenManager : GitHubAccessTokenManager {
  override suspend fun acquireAccessToken(installationId: Long): String? {
    return null
  }
}
