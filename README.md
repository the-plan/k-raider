# k-raider

```
sealed class Result<out A, out B> {
  class Success<B>(val value: B) : Result<Nothing, B>()
  class Failure<A>(val value: A) : Result<A, Nothing>()
}

fun divide(a: Int, b: Int): Result<Exception, Int> {
  try {
    return Result.Success<Int>(a/b)
  } catch (e: Exception) {
    return Result.Failure<Exception>(e)
  }
}



    println("----->")

    val r = divide(30,4)

    when(r) {
      is Result.Success<Int> -> println(r.value)
      is Result.Failure<Exception> -> println(r.value.message)
    }


    divide(30, 0).let {
      when (it) {
        is Result.Success<Int> -> println(it.value)
        is Result.Failure<Exception> -> println(it.value.message)
      }
    }
```


```
127.0.0.1       localhost
127.0.0.1       zeiracorp
127.0.1.1       zeiracorp
0.0.0.0       zeiracorp
255.255.255.255 broadcasthost
::1             localhost zeiracorp
::1             localhost zeiracorp.local
::1             localhost
# Bridged

```