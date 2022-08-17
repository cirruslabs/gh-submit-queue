package org.cirruslabs.sq

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.netty.handler.codec.DefaultHeaders
import org.cirruslabs.sq.github.GithubAppSecrets
import org.cirruslabs.sq.github.impl.GitHubAPIImpl
import org.cirruslabs.sq.github.impl.GitHubAccessTokenManagerImpl
import org.cirruslabs.sq.github.impl.GithubAppSecretsImpl

fun main() {
  embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
    configureRouting()
  }.start(wait = true)
}
fun Application.configureRouting() {
  val githubSecrets: GithubAppSecrets = GithubAppSecretsImpl.initialize()
  val gitHubScheme: String = System.getenv().getOrDefault("GITHUB_API_SCHEME", "https")
  val gitHubHost: String = System.getenv().getOrDefault("GITHUB_API_HOST", "api.github.com")
  val tokenManager = GitHubAccessTokenManagerImpl(
    baseAPIScheme = gitHubScheme,
    baseAPIHost = gitHubHost,
    secrets = githubSecrets
  )
  val api = GitHubAPIImpl(tokenManager, gitHubScheme, gitHubHost)
  mainWithApp(SubmitQueueApplication(api, githubSecrets))
}

fun Application.mainWithApp(app: SubmitQueueApplication) {
  app.apply { configureRouting() }
}
