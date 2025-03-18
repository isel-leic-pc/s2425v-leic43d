package pt.isel.pc.jht.synchronizers.cancellable

import java.lang.Thread.sleep
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.Condition
import java.util.LinkedList
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.jvm.Throws
import kotlin.time.Duration

// Using monitor-style.
// Allows acquiring and releasing multiple units.
// Threads attended in no particular order.
// Supports cancellation of blocking operations,
// either by timeout or interruption.
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
// Supports cancellation of blocking operations,
// either by timeout or interruption.
//
class BasicFairSemaphoreInKernelStyle(private var permits: Int) {
    private val locker = ReentrantLock()

    private inner class Request(
        val condition: Condition = locker.newCondition(),
        var done: Boolean = false
    )

    private val requests = LinkedList<Request>()

    @Throws(InterruptedException::class)
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
            try {
                do {
                    myRequest.condition.await()
                } while (!myRequest.done)
            } catch (ie : InterruptedException) {
                if (myRequest.done) {
                    Thread.currentThread().interrupt()
                    return
                }
                requests.remove(myRequest)
                throw ie
            }
        }
    }

    @Throws(InterruptedException::class)
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
            try {
                while (true) {
                    remainingTime = myRequest.condition.awaitNanos(remainingTime)

                    if (myRequest.done) {
                        return true
                    }

                    if (remainingTime <= 0) {
                        requests.remove(myRequest)
                        return false  // TIMEOUT
                    }
                }
            } catch (ie : InterruptedException) {
                if (myRequest.done) {
                    Thread.currentThread().interrupt()
                    return true
                }
                requests.remove(myRequest)
                throw ie
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

fun main() {
    val go = CountDownLatch(1)

    // The behaviour of this program should be similar
    // irrespectively of the version used, as it uses
    // single unit operations and does not depend on
    // fairness.
    val sem = BasicSemaphoreInMonitorStyle(2)
    //val sem = BasicFairSemaphoreInKernelStyle(2)

    fun thFun(name : String) {
        println("$name running")
        go.await()

        println("$name acquiring")
        try {
            sem.acquire()
            println("$name acquired")
        } catch (iex : InterruptedException) {
            println("$name INTERRUPTED")
        }

        println("$name DONE")
    }

    val th1 = thread { thFun("Th1") }
    val th2 = thread { thFun("Th2") }
    val th3 = thread { thFun("Th3") }

    println("Th0 releasing other threads")
    go.countDown()

    sleep(5000)

    println("Th0 interrupting all threads")
    th1.interrupt()
    th2.interrupt()
    th3.interrupt()

    // Try running this code with and without this sleep.
    // The results may vary and possibly surprise you.
    sleep(2000)

    println("Th0 adding unit to semaphore")
    sem.release()

    th1.join()
    th2.join()
    th3.join()

    println("Th0 DONE")
}
