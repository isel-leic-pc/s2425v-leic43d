package pt.isel.pc.jht.coroutines.launch

import kotlinx.coroutines.*

val tid : Long
	inline get() = Thread.currentThread().threadId()

fun main() {
	runBlocking {

		launch { 
			delay(1500)
			println("++ [T$tid] Message 3 ++")
			delay(1000)
			println("++ [T$tid] Message 5 ++")
		}

		launch {
			delay(1000)
			println("++ [T$tid] Message 2 ++")
			Thread.sleep(5000)
			delay(1000)
			println("++ [T$tid] Message 4 ++")
		}

		launch {
			delay(500)
			println("++ [T$tid] Message 1 ++")
			delay(2500)
			println("++ [T$tid] Message 6 ++")
		}

		println(":: [T$tid] STARTING ::")
	}
}

// Try replacing one or more calls to delay with
// this atrocious_delay and check the differences
fun atrocious_delay(msDelay: Int) : Unit {
	val endTime = System.currentTimeMillis() + msDelay
	while (System.currentTimeMillis() < endTime) {
		// atrocious active wait
	}
}

// Try replacing one or more calls to delay with
// this braindead_delay and check the differences
fun braindead_delay(msDelay: Long) : Unit {
	Thread.sleep(msDelay)
}

