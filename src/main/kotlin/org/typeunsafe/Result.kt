package org.typeunsafe

sealed class Result<out A, out B> {
  class Success<B>(val value: B) : Result<Nothing, B>()
  class Failure<A>(val value: A) : Result<A, Nothing>()
}