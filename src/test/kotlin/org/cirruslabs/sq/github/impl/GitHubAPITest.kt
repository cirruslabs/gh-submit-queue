package org.cirruslabs.sq.github.impl

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.cirruslabs.sq.github.api.CheckRunConclusion
import org.cirruslabs.sq.github.api.CheckSuiteConclusion
import org.cirruslabs.sq.github.api.PullRequestState
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GitHubAPITest {
  private val api = GitHubAPIImpl(
    TestGitHubAccessTokenManager,
    httpClient = HttpClient(CIO) {
      install(ContentNegotiation) {
        gson()
      }
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
        owner = "flutter",
        repo = "flutter",
        ref = "acd51a726e7c2eeb0e077890cd7b2f4f3bbc4931"
      ).toList()
      val cirrusSuite = suites.find { it.app.name == "Cirrus CI" }
      assertNotNull(cirrusSuite)
      assertEquals(CheckSuiteConclusion.success, cirrusSuite.conclusion)

      val wipSuite = suites.find { it.app.name == "WIP" }
      assertNotNull(wipSuite)
      assertTrue(wipSuite.notInitialized)
    }
  }

  @Test
  fun listCheckRuns() {
    runBlocking {
      val runs = api.listCheckRuns(
        installationId = 0L,
        owner = "flutter",
        repo = "plugins",
        checkSuiteId = 600645028
      ).toList()
      val failedRun = runs.find { !it.successful }
      assertNotNull(failedRun)
      assertEquals("analyze", failedRun.name)
      assertEquals(CheckRunConclusion.failure, failedRun.conclusion)
      assertEquals("https://github.com/flutter/plugins/runs/589861315", failedRun.html_url)
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
