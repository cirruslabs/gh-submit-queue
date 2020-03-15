package org.cirruslabs.sq

import io.ktor.application.Application
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.cirruslabs.sq.github.GitHubAPI
import org.cirruslabs.sq.github.GithubAppSecrets
import org.cirruslabs.sq.github.impl.GitHubAPIImpl
import org.cirruslabs.sq.github.impl.GitHubAccessTokenManagerImpl
import org.cirruslabs.sq.github.impl.GithubAppSecretsImpl

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
fun Application.main() {
  val githubSecrets: GithubAppSecrets = GithubAppSecretsImpl.initialize()
  val gitHubScheme: String = System.getenv().getOrDefault("GITHUB_API_SCHEME", "https")
  val gitHubHost: String = System.getenv().getOrDefault("GITHUB_API_HOST", "api.github.com")
  val httpClient: HttpClient = HttpClient(CIO) {
    install(JsonFeature)
  }
  val tokenManager = GitHubAccessTokenManagerImpl(
    baseAPIScheme = gitHubScheme,
    baseAPIHost = gitHubHost,
    secrets = githubSecrets,
    httpClient = httpClient
  )
  val api = GitHubAPIImpl(tokenManager, gitHubScheme, gitHubHost, httpClient)
  mainWithApp(SubmitQueueApplication(api, githubSecrets))
}

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
fun Application.mainWithApp(app: SubmitQueueApplication) {
  app.apply { main() }
}
