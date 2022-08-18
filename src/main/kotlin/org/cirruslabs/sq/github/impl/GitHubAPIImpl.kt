package org.cirruslabs.sq.github.impl

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.cirruslabs.sq.github.GitHubAPI
import org.cirruslabs.sq.github.GitHubAccessTokenManager
import org.cirruslabs.sq.github.api.*


class GitHubAPIImpl constructor(
  private val accessTokenManager: GitHubAccessTokenManager,
  private val baseAPIScheme: String = "https",
  private val baseAPIHost: String = "api.github.com",
  private val httpClient: HttpClient = HttpClient(CIO) {
    expectSuccess = false // to have 404 and etc
    engine {
      endpoint {
        connectAttempts = 3
      }
    }
    install(ContentNegotiation) {
      gson {
        setPrettyPrinting()
        setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
      }
    }
    install(HttpTimeout) {
      requestTimeoutMillis = 60_000
    }
    install(UserAgent) {
      agent = "CirrusCI"
    }
  }
) : GitHubAPI {
  companion object {
    private val CONTENT_TYPE_ANTIOPE_PREVIEW = ContentType("application", "vnd.github.antiope-preview+json")
    private val CONTENT_TYPE_SAILOR_V_PREVIEW = ContentType("application", "vnd.github.sailor-v-preview+json")
  }

  private suspend fun get(installationId: Long, path: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
    return httpClient.get(
      "$baseAPIScheme://$baseAPIHost/${path.removePrefix("/")}"
    ) {
      authorize(installationId)
      apply(block)
    }
  }

  private suspend fun post(
    installationId: Long,
    path: String,
    body: Any = EmptyContent,
    block: HttpRequestBuilder.() -> Unit = {}
  ): HttpResponse {
    return httpClient.post(
      "$baseAPIScheme://$baseAPIHost/${path.removePrefix("/")}"
    ) {
      setBody(body)
      authorize(installationId)
      // always post JSON
      contentType(ContentType.Application.Json)
      apply(block)
    }
  }

  private suspend fun HttpRequestBuilder.authorize(installationId: Long) {
    accept(ContentType.Application.Json)
    val accessToken = accessTokenManager.acquireAccessToken(installationId)
    if (accessToken != null) {
      header(HttpHeaders.Authorization, "token $accessToken")
    }
  }

  private fun noMorePages(response: HttpResponse) =
    response.headers.getAll(HttpHeaders.Link)?.size != 2

  override suspend fun listCheckSuites(
    installationId: Long,
    owner: String,
    repo: String,
    ref: String
  ): Flow<CheckSuite> {
    return flow {
      val infinitePagesSequence = generateSequence(1) { it + 1 }
      for (page in infinitePagesSequence) {
        val response = get(
          installationId = installationId,
          path = "/repos/$owner/$repo/commits/$ref/check-suites"
        ) {
          accept(CONTENT_TYPE_ANTIOPE_PREVIEW)
          parameter("page", page.toString())
        }
        emitAll(response.body<CheckSuitesResponse>().check_suites.asFlow())
        if (noMorePages(response)) {
          break
        }
      }
    }
  }

  override suspend fun listCheckRuns(
    installationId: Long,
    owner: String,
    repo: String,
    checkSuiteId: Long
  ): Flow<CheckRun> {
    return flow {
      val infinitePagesSequence = generateSequence(1) { it + 1 }
      for (page in infinitePagesSequence) {
        val response = get(
          installationId = installationId,
          path = "/repos/$owner/$repo/check-suites/$checkSuiteId/check-runs"
        ) {
          accept(CONTENT_TYPE_ANTIOPE_PREVIEW)
          parameter("page", page.toString())
        }
        emitAll(response.body<CheckRunsResponse>().check_runs.asFlow())
        if (noMorePages(response)) {
          break
        }
      }
    }
  }

  override suspend fun listPullRequests(
    installationId: Long,
    owner: String,
    repo: String,
    params: Map<String, String>
  ): Flow<PullRequest> {
    return flow {
      val infinitePagesSequence = generateSequence(1) { it + 1 }
      for (page in infinitePagesSequence) {
        val response = get(
          installationId = installationId,
          path = "/repos/$owner/$repo/pulls"
        ) {
          accept(CONTENT_TYPE_SAILOR_V_PREVIEW)
          parameter("page", page.toString())
          params.forEach { (name, value) ->
            parameter(name, value)
          }
        }
        emitAll(response.body<Array<PullRequest>>().asFlow())
        if (noMorePages(response)) {
          break
        }
      }
    }
  }

  override suspend fun setStatus(
    installationId: Long,
    owner: String,
    repo: String,
    sha: String,
    status: Status
  ): Status {
    val response = post(
      installationId = installationId,
      path = "/repos/$owner/$repo/statuses/$sha",
      body = status
    )

    if (response.status != HttpStatusCode.Created) {
      System.err.println("Failed to create status $status for $owner/$repo@$sha: ${response.bodyAsText()}")
      throw IllegalStateException("Failed to create status $status for $owner/$repo@$sha!")
    }
    return response.body()
  }
}
