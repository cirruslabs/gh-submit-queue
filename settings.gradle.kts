rootProject.name = "gh-submit-queue"

val isCiServer = System.getenv().containsKey("CIRRUS_CI")
val isMasterBranch = System.getenv()["CIRRUS_BRANCH"] == "master"
val buildCacheHost = System.getenv().getOrDefault("CIRRUS_HTTP_CACHE_HOST", "localhost:12321")

buildCache {
  local {
    isEnabled = !isCiServer
  }
  remote<HttpBuildCache> {
    uri("http://${buildCacheHost}/")
    isEnabled = isCiServer
    isPush = isMasterBranch
  }
}
