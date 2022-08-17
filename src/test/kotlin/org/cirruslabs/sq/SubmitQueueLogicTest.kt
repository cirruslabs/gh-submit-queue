package org.cirruslabs.sq

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.runBlocking
import org.cirruslabs.sq.github.impl.GitHubAPIImpl
import org.cirruslabs.sq.github.impl.TestGitHubAccessTokenManager
import org.cirruslabs.sq.model.Conclusion
import org.junit.Test
import kotlin.test.assertEquals

class SubmitQueueLogicTest {
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
  private val logic = SubmitQueueLogic(api)

  @Test
  fun overallConclusion() {
    runBlocking {
      val conclusion = logic.overallConclusion(installationId = 0L,
        owner = "flutter",
        repo = "flutter",
        ref = "acd51a726e7c2eeb0e077890cd7b2f4f3bbc4931")
      assertEquals(Conclusion(true), conclusion)
    }
  }
}
