package pt.isel.pc.jht.flowing.cold

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

val tid
    get() = Thread.currentThread().threadId()

suspend fun <T> collectFromFlow(theFlow: Flow<T>, message: String?) {
    println(":: COLLECTING${ message?.let { "($it)" } ?: "" } ::")
    theFlow.collect { value ->
        println("[T$tid] Received: $value")
    }
}

fun main() =
    runBlocking(Dispatchers.Default) {
        val locale = Locale.getDefault()
        println("[T$tid] Cold Flow example starting...")

        val myFlow1: Flow<String> = flow {
            println("[T$tid] Flow started")
            emit("IPL")
            delay(1000)
            emit("ISEL")
            delay(1000)
            emit("LEIC")
            delay(1000)
            emit("PC")
        }

        val myFlow2 = flowOf("Europa", "Portugal", "Lisboa", "Chelas").onEach { delay(1000) }

        val myFlow3 = listOf("Campus ISEL", "Edifício F", "Piso -1", "Sala LH3").asFlow().onEach { delay(1000) }

        launch { collectFromFlow(myFlow1, "Flow block (1)") }

        launch { collectFromFlow(myFlow2, "flowOf") }

        launch { collectFromFlow(myFlow1.map { it.lowercase() }, "Flow block (2)") }

        launch { collectFromFlow(myFlow3, "asFlow") }

        println("[T$tid] Done.")
    }
