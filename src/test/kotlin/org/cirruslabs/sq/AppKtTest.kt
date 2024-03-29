package org.cirruslabs.sq

import com.google.common.io.Resources
import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import org.cirruslabs.sq.github.GitHubAPI
import org.cirruslabs.sq.github.api.*
import org.junit.Test


class AppKtTest {
  val gson = Gson()

  fun runTest(test: TestApplicationEngine.(mockAPI: GitHubAPI) -> Unit) {
    val mockAPI = mockkClass(GitHubAPI::class)
    withTestApplication({ configureRoutingWithApp(mockAPI) }) {
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
        mockAPI.listPullRequests(
          102236L,
          "cirruslabs",
          "sandbox",
          mapOf("base" to "master", "state" to "open", "sort" to "updated", "direction" to "desc")
        )
        val expectedStatus = Status(
          StatusState.failure,
          "Cirrus CI failure on master",
          target_url = "https://github.com/cirruslabs/sandbox/runs/504360682"
        )
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
        val expectedStatus = Status(
          StatusState.failure,
          "Cirrus CI failure on master",
          target_url = "https://github.com/cirruslabs/sandbox/runs/504360682"
        )
        mockAPI.setStatus(102236L, "cirruslabs", "sandbox", "5687afbadb49ea7fd8fca74efbf88e7bb48e123a", expectedStatus)
      }

      confirmVerified(mockAPI)
    }
  }

  @Test
  fun issuePoked() {
    runTest { mockAPI ->
      val checkSuitesResponse = Resources.getResource("pull_request/check-suites.response.json").readText()
      val checkRunsResponse = Resources.getResource("pull_request/check-runs.response.json").readText()
      val prInfoResponse = Resources.getResource("issue_comment/prInfo.response.json").readText()
      val hookPayload = Resources.getResource("issue_comment/created.hook.json").readBytes()

      coEvery {
        mockAPI.listCheckSuites(102236L, "cirruslabs", "sandbox", "master")
      } returns gson.fromJson(checkSuitesResponse, CheckSuitesResponse::class.java).check_suites.asFlow()
      coEvery {
        mockAPI.listCheckRuns(102236L, "cirruslabs", "sandbox", 517820163)
      } returns gson.fromJson(checkRunsResponse, CheckRunsResponse::class.java).check_runs.asFlow()
      coEvery {
        mockAPI.prInfo(102236L, "cirruslabs", "sandbox", 9)
      } returns gson.fromJson(prInfoResponse, PullRequest::class.java)
      coEvery {
        mockAPI.setStatus(102236L, "cirruslabs", "sandbox", any(), any())
      } returns Status()

      handleRequest(HttpMethod.Post, "/hooks/github") {
        addHeader("X-GitHub-Delivery", "test")
        addHeader("X-GitHub-Event", "issue_commented")
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(hookPayload)
      }

      coVerifyOrder {
        mockAPI.prInfo(102236L, "cirruslabs", "sandbox", 9)
        mockAPI.listCheckSuites(102236L, "cirruslabs", "sandbox", "master")
        mockAPI.listCheckRuns(102236L, "cirruslabs", "sandbox", 517820163)
        val expectedStatus = Status(
          StatusState.failure,
          "Cirrus CI failure on master",
          target_url = "https://github.com/cirruslabs/sandbox/runs/504360682"
        )
        mockAPI.setStatus(102236L, "cirruslabs", "sandbox", "12f50eac2752768d664c9c1fc2cf99917438af56", expectedStatus)
      }

      confirmVerified(mockAPI)
    }
  }

}
