package pt.isel.pc.jht.synchronizers.kernel

import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.Condition
import java.util.LinkedList
import kotlin.concurrent.withLock
import kotlin.time.Duration

// A readers-writers lock allows multiple readers to be reading
// at the same time, while still keeping writers with exclusive
// access.
//
// In this implementation, writers have priority over readers.
// Writers still have to wait for their turn to get access, in
// case of ongoing reads, but if a writer is waiting for access,
// any new incoming reader will have to wait too.

// Expected usage:
//
// READERS:
// rwlock.beginRead()
// try {
//    .... read ....
// } finally {
//    rwlock.endRead()
// }
//
// WRITERS:
// rwlock.beginWrite()
// try {
//    .... write ....
// } finally {
//    rwlock.endWrite()
// }
//
// This example demonstrates several previously discussed techniques applied
// in implementing a synchronizer with a complex specification.
// One technique is the ordered servicing of writers using individual requests,
// while handling readers in groups, with multiple pending operations sharing a
// common request.
// Additionally, note the use of "action delegation": the operation that signals
// a writer to proceed proactively executes the writer's action before releasing
// the exclusion lock. Here, the isWriting flag is set along with the signal dispatch,
// not when the writer wakes up. Similarly, when signaling a group of blocked readers,
// the numReaders counter is updated immediately.
// With action delegation, if requests are simultaneously resolved and cancelled,
// the success of the operation takes priority. For cancellations via interrupts,
// this may require absorbing the InterruptedException and reinstating the interrupt.
// Also, canceling a 'beginWrite' may allow other pending requests to proceed;
// consequently, the cancellation code might need to resolve and unblock those pending
// requests. Unlike previous examples, instead of waitingWriters.remove(myRequest),
// this implementation employs quitWaitingForWrite(myRequest) to handle those cases.
//
class RWLock {
    private val locker = ReentrantLock()
    private var isWriting = false
    private var numReaders = 0

    private inner class WriteRequest {
        val condition : Condition = locker.newCondition()
        var done : Boolean = false
    }

    private inner class ReadRequest {
        var numWaiting : Int = 0
        val condition : Condition = locker.newCondition()
        var done : Boolean = false
    }

    private val waitingWriters = LinkedList<WriteRequest>()
    private var currReadRequest = ReadRequest()

    @Throws(InterruptedException::class)
    fun beginRead(timeout: Duration) : Boolean {
        locker.withLock {
            // fast-path
            if (!isWriting && waitingWriters.isEmpty()) {
                numReaders++
                return true
            }

            // wait-path
            val myRequest = currReadRequest
            myRequest.numWaiting++
            try {
                var remainingTime = timeout.inWholeNanoseconds
                while (true) {
                    remainingTime = myRequest.condition.awaitNanos(remainingTime)
                    if (myRequest.done) {
                        return true
                    }
                    if (remainingTime <= 0) {
                        quitWaitingForRead(myRequest)
                        return false
                    }
                }
            } catch (ie: InterruptedException) {
                if (myRequest.done) {
                    Thread.currentThread().interrupt()
                    return true
                }
                quitWaitingForRead(myRequest)
                throw ie
            }
        }
    }

    private fun quitWaitingForRead(myRequest: ReadRequest) {
        myRequest.numWaiting--
    }

    fun endRead() {
        locker.withLock {
            if (--numReaders == 0) {  // last reader
                unblockFirstWaitingWriter()
            }
        }
    }

    @Throws(InterruptedException::class)
    fun beginWrite(timeout: Duration) : Boolean {
        locker.withLock {
            // fast-path
            if (!isWriting && numReaders == 0) {
                isWriting = true
                return true
            }

            // wait-path
            val myRequest = WriteRequest()
            waitingWriters.addLast(myRequest)
            try {
                var remainingTime = timeout.inWholeNanoseconds
                while (true) {
                    remainingTime = myRequest.condition.awaitNanos(remainingTime)
                    if (myRequest.done) {
                        return true
                    }
                    if (remainingTime <= 0) {
                        quitWaitingForWrite(myRequest)
                        return false
                    }
                }
            } catch (ie: InterruptedException) {
                if (myRequest.done) {
                    Thread.currentThread().interrupt()
                    return true
                }
                quitWaitingForWrite(myRequest)
                throw ie
            }
        }
    }

    private fun quitWaitingForWrite(myRequest: WriteRequest) {
        waitingWriters.remove(myRequest)
        if (waitingWriters.isEmpty() && !isWriting) {
            unblockAllWaitingReaders()
        }
    }

    fun endWrite() {
        locker.withLock {
            isWriting = false
            if (waitingWriters.isNotEmpty()) {  // priority to writers
                unblockFirstWaitingWriter()
            } else {
                unblockAllWaitingReaders()
            }
        }
    }

    private fun unblockFirstWaitingWriter() {
        if (waitingWriters.isNotEmpty()) {
            val firstWriter = waitingWriters.removeFirst()
            isWriting = true  // execution delegation
            firstWriter.done = true
            firstWriter.condition.signal()
        }
    }

    private fun unblockAllWaitingReaders() {
        if (currReadRequest.numWaiting > 0) {
            numReaders += currReadRequest.numWaiting  // execution delegation
            currReadRequest.done = true
            currReadRequest.condition.signalAll()
            currReadRequest = ReadRequest()
        }
    }
}
