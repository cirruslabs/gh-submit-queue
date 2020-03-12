package org.cirruslabs.sq.github.impl

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.cirruslabs.sq.github.api.PullRequestState
import org.junit.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
class GitHubAPITest {
  private val api = GitHubAPIImpl(TestGitHubAccessTokenManager)

  @Test
  fun listPRs() {
    runBlocking {
      val prs = api.listPullRequests(
        installationId = 0L,
        owner = "flutter",
        repo = "flutter",
        params = mapOf(
          "base" to "master",
          "state" to "closed",
          "sort" to "created",
          "direction" to "asc"
        )
      )
      val oldestPR = prs.first()
      assertEquals(1, oldestPR.number)
      assertEquals(PullRequestState.closed, oldestPR.state)
    }
  }
}
