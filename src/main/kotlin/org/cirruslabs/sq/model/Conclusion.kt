package org.cirruslabs.sq.model

data class Conclusion(
    val completed: Boolean,
    val failureDetails: ConclusionDetails? = null
)

data class ConclusionDetails(
  val appName: String,
  val url: String,
  val status: String
)
