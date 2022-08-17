package org.cirruslabs.sq.github.impl

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import org.cirruslabs.sq.github.GitHubAccessTokenManager
import org.cirruslabs.sq.github.GithubAppSecrets
import org.cirruslabs.sq.github.api.AccessTokenResponse

class GitHubAccessTokenManagerImpl constructor(
  private val baseAPIScheme: String = "https",
  private val baseAPIHost: String = "api.github.com",
  private val secrets: GithubAppSecrets,
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
) : GitHubAccessTokenManager {
  companion object {
    private val CONTENT_TYPE_MACHINE_MAN_PREVIEW = ContentType("application", "vnd.github.machine-man-preview+json")
  }

  private val accessTokenCache: Cache<Long, AccessTokenResponse> = Caffeine.newBuilder()
    .expireAfter(object : Expiry<Long, AccessTokenResponse> {
      override fun expireAfterUpdate(key: Long, value: AccessTokenResponse, currentTime: Long, currentDuration: Long): Long {
        return value.expiresIn.toNanos()
      }

      override fun expireAfterCreate(key: Long, value: AccessTokenResponse, currentTime: Long): Long {
        return value.expiresIn.toNanos()
      }

      override fun expireAfterRead(key: Long, value: AccessTokenResponse, currentTime: Long, currentDuration: Long): Long {
        return value.expiresIn.toNanos()
      }
    })
    .softValues()
    .build()

  override suspend fun acquireAccessToken(installationId: Long): String? {
    val cachedResponse = accessTokenCache.getIfPresent(installationId)
    if (cachedResponse != null) {
      return cachedResponse.token
    }
    val renewedResponse = acquireAccessTokenImpl(installationId)
    accessTokenCache.put(installationId, renewedResponse)
    return renewedResponse.token
  }

  private suspend fun acquireAccessTokenImpl(installationId: Long): AccessTokenResponse {
    val jwt = secrets.signJWT()
    val response = httpClient.post(
      "$baseAPIScheme://$baseAPIHost/app/installations/$installationId/access_tokens"
    ) {
      header(HttpHeaders.Authorization, "Bearer $jwt")
      accept(CONTENT_TYPE_MACHINE_MAN_PREVIEW)
    }
    if (response.status != HttpStatusCode.Created) {
      throw IllegalStateException("Failed to acquire an access token for installation $installationId!")
    }
    val accessTokenResponse = response.body<AccessTokenResponse>()
    println("Got a token for $installationId that expires in ${accessTokenResponse.expiresIn.toMinutes()} minutes.")
    return accessTokenResponse
  }
}
