package pt.isel.pc.jht.synchronizers.timeout

import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.Condition
import java.util.LinkedList
import kotlin.concurrent.withLock
import kotlin.jvm.Throws
import kotlin.time.Duration

// Using monitor-style.
// Allows acquiring and releasing multiple units.
// Threads attended in no particular order.
//
// Properly supports interrupts.
//
// Supports specifying a timeout in the blocking operation.
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

	@Throws(InterruptedException::class)
	fun acquire(units : Int, timeout: Duration) : Boolean {
		locker.withLock {
			var remainingTime = timeout.inWholeNanoseconds
			while (permits < units) {
				if (remainingTime <= 0) {
					return false  // TIMEOUT
				}
				remainingTime = waitSet.awaitNanos(remainingTime)
			}
			permits -= units
			return true
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
// Supports specifying a timeout in the blocking operation.
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

	fun acquire(timeout: Duration) : Boolean {
		locker.withLock {
			// fast-path
			if (permits > 0) {
				permits -= 1
				return true
			}
			if (timeout == Duration.ZERO) {
				return false  // TIMEOUT
			}

			// wait-path
			var remainingTime = timeout.inWholeNanoseconds
			val myRequest = Request()
			requests.addLast(myRequest)
			while (true) {
				try {
					remainingTime = myRequest.condition.awaitNanos(remainingTime)
				} catch (_ : InterruptedException) {
					// WARNING!
					// Interrupts are ignored.
					// This will be corrected in a later version.
				}

				if (myRequest.done) {
					return true
				}

				if (remainingTime <= 0) {
					requests.remove(myRequest)
					return false  // TIMEOUT
				}
			}
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
