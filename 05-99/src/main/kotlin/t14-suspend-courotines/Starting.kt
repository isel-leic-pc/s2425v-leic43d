import kotlin.coroutines.*

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val tid : Long
	inline get() = Thread.currentThread().threadId()

suspend fun demoFunction(): Int {
	println("T$tid: begin of demoFunction")
	val res = 8
	println("T$tid: end of demoFunction")
	return res
}

fun main() {
    val continuation = object : Continuation<Int> {
        override val context: CoroutineContext = EmptyCoroutineContext

        override fun resumeWith(result: Result<Int>) {
            println("T$tid: demo result -> $result")    
        }
    }

    ::demoFunction.startCoroutine(continuation)
    println("T$tid: coroutine started")
}