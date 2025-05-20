package pt.isel.pc.jht.flowing.basic

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

suspend fun <T> collectFromFlow(theFlow: Flow<T>, message: String?) {
    println(":: COLLECTING${ message?.let { "($it)" } ?: "" } ::")
    theFlow.collect { value ->
        println("Received: $value")
    }
}

fun main() = runBlocking {
    println("Cold Flow example starting...")

    val myFlow1: Flow<String> = flow {
        println("Flow started")
        emit("IPL")
        delay(1000)
        emit("ISEL")
        delay(1000)
        emit("LEIC")
        delay(1000)
        emit("PC")
    }

    val myFlow2 = flowOf("IPL", "ISEL", "LEIC", "PC")

    val myFlow3 = listOf("IPL", "ISEL", "LEIC", "PC").asFlow()

    collectFromFlow(myFlow1, "Flow block (1)")

    collectFromFlow(myFlow2, "flowOf")

    collectFromFlow(myFlow1, "Flow block (2)")

    collectFromFlow(myFlow3, "asFlow")

    println("Done.")
}
