package io.norselabs.vpn.common_network.cache

class CacheUnit<Input, Output>(
  private val fetcher: suspend (Input) -> Output,
) {

  private val cache = mutableMapOf<Input, Output>()

  suspend fun get(
    input: Input,
    isFresh: Boolean = false,
  ): Output {
    if (isFresh) {
      cache.clear()
    }
    return cache.getOrPut(input) {
      fetcher(input)
    }
  }
}
