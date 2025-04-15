package pt.isel.pc.jht.coroutines.hello

import kotlinx.coroutines.*

val tid : Long
	inline get() = Thread.currentThread().threadId()

fun main() = runBlocking {

	launch {
		delay(5000)
		println(", world! [T$tid]")
	}

	print("[T$tid] Hello")
}
