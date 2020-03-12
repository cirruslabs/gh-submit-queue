plugins {
  application
  kotlin("jvm") version "1.3.70"
  id("com.google.cloud.tools.jib") version "2.1.0"
}

repositories {
  jcenter()
}

jib {
  to {
    image = "gcr.io/submit-queue-app/gh-submit-queue"
    tags = mutableSetOf("latest")
  }
}

application {
  mainClassName = "org.cirruslabs.sq.AppKt"
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

val ktorVersion = "1.3.1"

dependencies {
  implementation("com.auth0:java-jwt:3.10.0")
  implementation("com.github.ben-manes.caffeine:caffeine:2.8.1")
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
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")

  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}