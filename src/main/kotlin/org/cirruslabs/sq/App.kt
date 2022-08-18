package org.cirruslabs.sq

import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.cirruslabs.sq.github.GitHubAPI
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
  configureRoutingWithApp(api, githubSecrets)
}

fun Application.configureRoutingWithApp(
  api: GitHubAPI,
  githubSecrets: GithubAppSecrets? = null
) {
  install(DefaultHeaders)
  install(DoubleReceive)
  install(ContentNegotiation) {
    gson {
      setPrettyPrinting()
    }
  }
  install(StatusPages) {
    exception<Throwable> { call: ApplicationCall, cause: Throwable ->
      // log errors
      cause.printStackTrace()
      call.respond(HttpStatusCode.InternalServerError, cause.message ?: "")
    }
  }
  routing {
    SubmitQueueApplication(api, githubSecrets).setup(this)
  }
}
