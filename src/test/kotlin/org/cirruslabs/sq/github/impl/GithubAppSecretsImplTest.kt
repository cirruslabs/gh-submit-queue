package org.cirruslabs.sq.github.impl

import com.auth0.jwt.algorithms.Algorithm
import com.google.common.io.Resources
import org.junit.Test
import kotlin.test.assertEquals

class GithubAppSecretsImplTest {
  @Test
  fun signature() {
    val secrets = GithubAppSecretsImpl(
      0L,
      "",
      "",
      Algorithm.none(),
      "3333333333333333333333333333333333333333"
    )

    assertEquals("sha1=3502e056e9ef6592ae6b09f9f25a7b841b4a327b", secrets.calculateSignature("{}".toByteArray()))
  }
}
