package org.cirruslabs.sq.github.impl

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.util.KtorExperimentalAPI
import org.cirruslabs.sq.github.GitHubAccessTokenManager
import org.cirruslabs.sq.github.GithubAppSecrets
import org.cirruslabs.sq.github.api.AccessTokenResponse

class GitHubAccessTokenManagerImpl @KtorExperimentalAPI constructor(
  private val baseAPIScheme: String = "https",
  private val baseAPIHost: String = "api.github.com",
  private val secrets: GithubAppSecrets,
  private val httpClient: HttpClient = HttpClient(CIO) {
    install(JsonFeature)
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
    val response = httpClient.post<HttpResponse>(
      scheme = baseAPIScheme,
      host = baseAPIHost,
      path = "/installations/$installationId/access_tokens"
    ) {
      header(HttpHeaders.Authorization, "Bearer $jwt")
      accept(CONTENT_TYPE_MACHINE_MAN_PREVIEW)
    }
    if (response.status != HttpStatusCode.Created) {
      throw IllegalStateException("Failed to acquire an access token for installation $installationId!")
    }
    val accessTokenResponse = response.call.receive<AccessTokenResponse>()
    println("Got a token for $installationId that expires in ${accessTokenResponse.expiresIn.toMinutes()} minutes.")
    return accessTokenResponse
  }
}
