package pt.isel.pc.jht.suspending.pausing

import kotlin.coroutines.*

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val tid : Long
	inline get() = Thread.currentThread().threadId()

private val executor = Executors.newSingleThreadScheduledExecutor()

suspend fun pause(time: Long) {
    suspendCoroutine<Unit> { continuation : Continuation<Unit> ->
        executor.schedule({
            continuation.resume(Unit)
        }, time, TimeUnit.MILLISECONDS)
    }
}

suspend fun delayedSum(op1: Int, op2: Int, delta: Long): Int {
	println("T$tid: begin of delayedSum")
	pause(delta)
	println("T$tid: end of delayedSum")
    return op1 + op2
}

suspend fun demoFunction(): Int {
	println("T$tid: begin of demoFunction")
	val res = delayedSum(3, 5, 8000L)
	println("T$tid: end of demoFunction")
	return res
}

fun main() {
    val continuation = object : Continuation<Int> {
        override val context: CoroutineContext = EmptyCoroutineContext

        override fun resumeWith(result: Result<Int>) {
            println("T$tid: demo result -> $result")
            executor.shutdown()
        }
    }

    ::demoFunction.startCoroutine(continuation)
    println("T$tid: coroutine started")
}