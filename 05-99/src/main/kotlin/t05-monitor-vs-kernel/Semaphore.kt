package pt.isel.pc.jht.synchronizers.styles

import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.Condition
import java.util.LinkedList
import kotlin.concurrent.withLock
import kotlin.jvm.Throws

// Using monitor-style.
// Allows acquiring and releasing multiple units.
// Threads attended in no particular order.
//
// Properly supports interrupts.
//
class BasicSemaphoreInMonitorStyle(private var permits: Int) {
	private val locker = ReentrantLock()
	private val waitSet = locker.newCondition()

	@Throws(InterruptedException::class)
	fun acquire(units : Int = 1) {
		locker.withLock {
			while (permits < units) {
				waitSet.await()
			}
			permits -= units
		}
	}

	fun release(units : Int = 1) {
		locker.withLock {
			permits += units
			waitSet.signalAll()
		}
	}
}

// Using kernel-style.
// Single unit acquires and releases.
// Threads are attended by order of arrival.
//
// WARNING!
// This version does not support interrupts!
//
class BasicFairSemaphoreInKernelStyle(private var permits: Int) {
	private val locker = ReentrantLock()

	private inner class Request(
		val condition: Condition = locker.newCondition(),
		var done: Boolean = false
	)

	private val requests = LinkedList<Request>()

	fun acquire() {
		locker.withLock {
			// fast-path
			if (permits > 0) {
				permits -= 1
				return
			}

			// wait-path
			val myRequest = Request()
			requests.addLast(myRequest)
			do {
				try {
					myRequest.condition.await()
				} catch (_ : InterruptedException) {
					// WARNING!
					// Interrupts are ignored.
					// This will be corrected in a later version.
				}
			} while (!myRequest.done)
		}
	}
	
	fun release() {
		locker.withLock {
			if (requests.isEmpty()) {
				permits += 1
			} else {
				val req = requests.removeFirst()
				req.done = true
				req.condition.signal()
			}
		}
	}
}
