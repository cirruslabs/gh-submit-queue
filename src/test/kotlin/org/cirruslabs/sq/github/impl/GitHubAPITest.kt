package org.cirruslabs.sq.github.impl

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.cirruslabs.sq.github.api.CheckSuiteConclusion
import org.cirruslabs.sq.github.api.PullRequestState
import org.junit.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
class GitHubAPITest {
  private val api = GitHubAPIImpl(
    TestGitHubAccessTokenManager,
    httpClient = HttpClient(CIO) {
      install(JsonFeature)
      install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL
      } // for debugging
    }
  )

  @Test
  fun listCheckSuites() {
    runBlocking {
      val suites = api.listCheckSuites(
        installationId = 0L,
        owner = "cirruslabs",
        repo = "sandbox",
        ref = "e8ea21f5940a9ddd74b566138753435a17579530"
      )
      val suite = suites.first { it.app.name == "Cirrus CI" }
      assertEquals(CheckSuiteConclusion.failure, suite.conclusion)
    }
  }

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
