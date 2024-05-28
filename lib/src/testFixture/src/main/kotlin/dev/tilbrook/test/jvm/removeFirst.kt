package dev.tilbrook.test.jvm

fun removeFirst(): MutableList<Int> {
  return mutableListOf(1, 2, 3).apply {
    removeFirst()
  }
}
