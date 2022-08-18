plugins {
  application
  kotlin("jvm") version "1.7.10"
  id("com.google.cloud.tools.jib") version "3.2.1"
}

repositories {
  mavenCentral()
}

application {
  mainClass.set("org.cirruslabs.sq.AppKt")
}

jib {
  container {
    labels.put("org.opencontainers.image.source", "https://github.com/cirruslabs/gh-submit-queue/")
  }
  to {
    if (System.getenv().containsKey("GITHUB_ACTIONS")) {
      image = "ghcr.io/cirruslabs/gh-submit-queue"
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

tasks.withType<Jar> {
  manifest {
    attributes(
      mapOf(
        "Main-Class" to application.mainClassName
      )
    )
  }
}

val ktorVersion = "2.1.0"

dependencies {
  implementation("com.auth0:java-jwt:4.0.0")
  implementation("com.github.ben-manes.caffeine:caffeine:3.1.1")
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-cio:$ktorVersion")
  implementation("io.ktor:ktor-client-gson:$ktorVersion")
  implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
  implementation("io.ktor:ktor-serialization-gson:$ktorVersion")
  implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
  implementation("io.ktor:ktor-server-core:$ktorVersion")
  implementation("io.ktor:ktor-server-netty:$ktorVersion")
  implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
  implementation("io.ktor:ktor-server-double-receive:$ktorVersion")
  implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
  implementation("joda-time:joda-time:2.11.0")
  implementation("org.bouncycastle:bcprov-jdk15on:1.70")

  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
  testImplementation("io.mockk:mockk:1.12.5")
  testImplementation("com.google.code.gson:gson:2.9.1")
  testImplementation("com.google.guava:guava:31.1-jre")

  testImplementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")
  testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}
