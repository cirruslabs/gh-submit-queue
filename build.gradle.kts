plugins {
  application
  kotlin("jvm") version "1.3.72"
  id("com.google.cloud.tools.jib") version "2.4.0"
}

repositories {
  jcenter()
}

jib {
  to {
    if (System.getenv().containsKey("GITHUB_ACTIONS")) {
      image = "docker.pkg.github.com/cirruslabs/gh-submit-queue/app"
      val githubRef = System.getenv().getOrDefault("GITHUB_REF", "refs/heads/master")
      tags = mutableSetOf(if (githubRef.startsWith("refs/tags/")) githubRef.removePrefix("refs/tags/") else "latest")
      auth {
        username = System.getProperty("DOCKER_USERNAME")
        password = System.getProperty("DOCKER_PASSWORD")
      }
    } else {
      image = "gcr.io/submit-queue-app/gh-submit-queue"
      tags = mutableSetOf("latest")
    }
  }
}

application {
  mainClassName = "io.ktor.server.netty.DevelopmentEngine"
}

tasks.withType<Jar> {
  manifest {
    attributes(
      mapOf(
        "Main-Class" to application.mainClassName
      )
    )
  }
}

val ktorVersion = "1.3.2"

dependencies {
  implementation("com.auth0:java-jwt:3.10.0")
  implementation("com.github.ben-manes.caffeine:caffeine:2.8.5")
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-cio:$ktorVersion")
  implementation("io.ktor:ktor-client-gson:$ktorVersion")
  implementation("io.ktor:ktor-gson:$ktorVersion")
  implementation("io.ktor:ktor-server-core:$ktorVersion")
  implementation("io.ktor:ktor-server-netty:$ktorVersion")
  implementation("joda-time:joda-time:2.10.5")
  implementation("org.bouncycastle:bcprov-jdk15on:1.64")

  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
  testImplementation("io.mockk:mockk:1.10.0")
  testImplementation("com.google.code.gson:gson:2.8.6")
  testImplementation("com.google.guava:guava:29.0-jre")

  testImplementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")
  testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}
