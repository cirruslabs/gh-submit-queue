package org.cirruslabs.sq.github.impl

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.util.InternalAPI
import io.ktor.util.decodeBase64
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.io.pem.PemReader
import org.cirruslabs.sq.github.GithubAppSecrets
import org.joda.time.DateTime
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStreamReader
import java.io.StringReader
import java.nio.charset.Charset
import java.security.KeyFactory
import java.security.Security
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class GithubAppSecretsImpl(
  override val appId: Long,
  override val clientId: String,
  val clientSecret: String,
  val algorithm: Algorithm,
  webhookSecret: String // https://developer.github.com/webhooks/securing/
) : GithubAppSecrets {
  companion object {
    fun initialize(): GithubAppSecretsImpl {
      Security.addProvider(BouncyCastleProvider())

      val privateKeyBase64Content = System.getenv("GITHUB_APP_PRIVATE_KEY_BASE64")
      val privateKeyContent = Base64.getDecoder().decode(privateKeyBase64Content)
      val reader = PemReader(InputStreamReader(ByteArrayInputStream(privateKeyContent)))
      val pemObject = reader.readPemObject()
        ?: throw IllegalStateException("Failed to created private key from environment variable!")

      val keySpec = PKCS8EncodedKeySpec(pemObject.content)
      val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec) as RSAPrivateKey

      return GithubAppSecretsImpl(
        System.getenv("GITHUB_APP_ID").toLong(),
        System.getenv("GITHUB_APP_CLIENT_ID"),
        System.getenv("GITHUB_APP_CLIENT_SECRET"),
        Algorithm.RSA256(null, privateKey),
        System.getenv("GITHUB_APP_WEBHOOK_SECRET")
      )
    }
  }

  private val signingKey = SecretKeySpec(webhookSecret.toByteArray(), "HmacSHA1")

  override fun signJWT(): String {
    return JWT.create()
      .withIssuedAt(DateTime.now().toDate())
      .withExpiresAt(DateTime.now().plusMinutes(10).toDate())
      .withIssuer(appId.toString())
      .sign(algorithm)
  }


  override fun calculateSignature(data: ByteArray): String {
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(signingKey)
    return "sha1=${toHexString(mac.doFinal(data))}"
  }

  private fun toHexString(bytes: ByteArray): String {
    val formatter = Formatter()
    bytes.forEach { formatter.format("%02x", it) }
    return formatter.toString()
  }
}
