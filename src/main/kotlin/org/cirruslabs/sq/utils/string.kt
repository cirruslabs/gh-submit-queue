package org.cirruslabs.sq.utils

// equal with a constant time to prevent from security attacks
fun String.constantTimeEquals(other: String): Boolean {
  if (length != other.length) {
    return false
  }
  var result = true
  for (i in (0 until length)) {
    if (this[i] != other[i]) {
      result = false
    }
  }
  return result
}
