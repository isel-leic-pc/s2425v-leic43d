package pt.isel.pc.jht.coroutines.schedule

import kotlin.coroutines.*

val tid : Long
    inline get() = Thread.currentThread().threadId()

val continuations = mutableListOf<Continuation<Unit>>()

suspend fun f1() {
    val strings = listOf("Hello", "world")
    println(":: STARTING f1 ::")
    strings.forEach {
        suspendCoroutine { cont ->
            continuations.addLast(cont)
        }
        println("[T$tid] f1: $it")
    }
}

suspend fun f2() {
    val strings = listOf("Olá", "mundo")
    println(":: STARTING f2 ::")
    strings.forEach {
        suspendCoroutine { cont ->
            continuations.addLast(cont)
        }
        println("[T$tid] f2: $it")
    }
}

fun main() {
    val nop = object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            // nothing
        }
    }

	// Version A: using startCoroutine
    //::f1.startCoroutine(nop)
    //::f2.startCoroutine(nop)

	// Version B: using createCoroutine
    //continuations.addLast(::f1.createCoroutine(nop))
    //continuations.addLast(::f2.createCoroutine(nop))

    while(continuations.isNotEmpty()) {
        val next = continuations.removeFirst()
        next.resumeWith(Result.success(Unit))
    }
}
