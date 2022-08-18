package org.cirruslabs.sq.github

import kotlinx.coroutines.flow.Flow
import org.cirruslabs.sq.github.api.CheckRun
import org.cirruslabs.sq.github.api.CheckSuite
import org.cirruslabs.sq.github.api.PullRequest
import org.cirruslabs.sq.github.api.Status

interface GitHubAPI {
  suspend fun listCheckSuites(installationId: Long, owner: String, repo: String, ref: String): Flow<CheckSuite>
  suspend fun listCheckRuns(installationId: Long, owner: String, repo: String, checkSuiteId: Long): Flow<CheckRun>
  suspend fun listPullRequests(
    installationId: Long,
    owner: String,
    repo: String,
    params: Map<String, String> = emptyMap()
  ): Flow<PullRequest>

  suspend fun setStatus(installationId: Long, owner: String, repo: String, sha: String, status: Status): Status
}
