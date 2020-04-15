package org.cirruslabs.sq

import com.google.common.io.Resources
import com.google.gson.Gson
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import org.cirruslabs.sq.github.GitHubAPI
import org.cirruslabs.sq.github.api.*
import org.junit.Test


@ExperimentalCoroutinesApi
@KtorExperimentalAPI
class AppKtTest {
  val gson = Gson()

  fun runTest(test: TestApplicationEngine.(mockAPI: GitHubAPI) -> Unit) {
    val mockAPI = mockkClass(GitHubAPI::class)
    withTestApplication({ mainWithApp(SubmitQueueApplication(mockAPI)) }) {
      test(mockAPI)
    }
  }

  @Test
  fun checkSuiteSinglePR() {
    runTest { mockAPI ->
      val checkSuitesResponse = Resources.getResource("check_suite/single_pr/check-suites.response.json").readText()
      val checkRunsResponse = Resources.getResource("check_suite/single_pr/check-runs.response.json").readText()
      val prsResponse = Resources.getResource("check_suite/single_pr/pulls.response.json").readText()
      val hookPayload = Resources.getResource("check_suite/single_pr/completed.hook.json").readBytes()

      coEvery {
        mockAPI.listCheckSuites(102236L, "cirruslabs", "sandbox", "master")
      } returns gson.fromJson(checkSuitesResponse, CheckSuitesResponse::class.java).check_suites.asFlow()
      coEvery {
        mockAPI.listCheckRuns(102236L, "cirruslabs", "sandbox", 517820163)
      } returns gson.fromJson(checkRunsResponse, CheckRunsResponse::class.java).check_runs.asFlow()
      coEvery {
        mockAPI.listPullRequests(102236L, "cirruslabs", "sandbox", any())
      } returns flowOf(*gson.fromJson(prsResponse, Array<PullRequest>::class.java))
      coEvery {
        mockAPI.setStatus(102236L, "cirruslabs", "sandbox", any(), any())
      } returns Status()

      handleRequest(HttpMethod.Post, "/hooks/github") {
        addHeader("X-GitHub-Delivery", "test")
        addHeader("X-GitHub-Event", "check_suite")
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(hookPayload)
      }

      coVerifyOrder {
        mockAPI.listCheckSuites(102236L, "cirruslabs", "sandbox", "master")
        mockAPI.listCheckRuns(102236L, "cirruslabs", "sandbox", 517820163)
        mockAPI.listPullRequests(102236L, "cirruslabs", "sandbox", mapOf("base" to "master", "state" to "open", "sort" to "updated", "direction" to "desc"))
        val expectedStatus = Status(StatusState.failure, "Cirrus CI failure on master", target_url = "https://github.com/cirruslabs/sandbox/runs/504360682")
        mockAPI.setStatus(102236L, "cirruslabs", "sandbox", "990e3dc578b8b1607e28dfa2d5353a276741d77c", expectedStatus)
      }

      confirmVerified(mockAPI)
    }
  }

  @Test
  fun prOpened() {
    runTest { mockAPI ->
      val checkSuitesResponse = Resources.getResource("pull_request/check-suites.response.json").readText()
      val checkRunsResponse = Resources.getResource("pull_request/check-runs.response.json").readText()
      val hookPayload = Resources.getResource("pull_request/opened.hook.json").readBytes()

      coEvery {
        mockAPI.listCheckSuites(102236L, "cirruslabs", "sandbox", "master")
      } returns gson.fromJson(checkSuitesResponse, CheckSuitesResponse::class.java).check_suites.asFlow()
      coEvery {
        mockAPI.listCheckRuns(102236L, "cirruslabs", "sandbox", 517820163)
      } returns gson.fromJson(checkRunsResponse, CheckRunsResponse::class.java).check_runs.asFlow()
      coEvery {
        mockAPI.setStatus(102236L, "cirruslabs", "sandbox", any(), any())
      } returns Status()

      handleRequest(HttpMethod.Post, "/hooks/github") {
        addHeader("X-GitHub-Delivery", "test")
        addHeader("X-GitHub-Event", "pull_request")
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(hookPayload)
      }

      coVerifyOrder {
        mockAPI.listCheckSuites(102236L, "cirruslabs", "sandbox", "master")
        mockAPI.listCheckRuns(102236L, "cirruslabs", "sandbox", 517820163)
        val expectedStatus = Status(StatusState.failure, "Cirrus CI failure on master", target_url = "https://github.com/cirruslabs/sandbox/runs/504360682")
        mockAPI.setStatus(102236L, "cirruslabs", "sandbox", "5687afbadb49ea7fd8fca74efbf88e7bb48e123a", expectedStatus)
      }

      confirmVerified(mockAPI)
    }
  }
}
