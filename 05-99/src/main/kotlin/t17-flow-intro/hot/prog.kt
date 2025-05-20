package `t17-flow-intro`.hot

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed class Event {
    data class Msg(val text: String) : Event()
    object Done : Event()
}
fun main() = runBlocking {

    val sharedFlow = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 0
    )

    // Produtor: começa imediatamente
    launch {
        val items = listOf(
            "1-Pg", "2-AED", "3-LIC", "4-IPW", "5-PSC", "6-TDS",
            "7-LAE", "8-LS", "9-PC", "10-DAW", "11-PDM", "12-TVS"
        )
        for (item in items) {
            println("Producing: $item")
            sharedFlow.emit(Event.Msg(item))
            delay(1500)
        }
        println("Producing: Done")
        sharedFlow.emit(Event.Done)
    }

    // Coletor 1
    val c1 = launch {
        delay(2000)
        println("\t\t→ Collector 1 starting")
        sharedFlow.collect { event ->
            when (event) {
                is Event.Msg -> println("\t\tCollector 1 received: ${event.text}")
                is Event.Done -> {
                    println("\t\tCollector 1 received: Done")
                    cancel() // termina este coletor
                }
            }
        }
    }

    // Coletor 2
    val c2 = launch {
        delay(5000)
        println("\t\t\t\t→ Collector 2 starting")
        sharedFlow.collect { event ->
            when (event) {
                is Event.Msg -> println("\t\t\t\tCollector 2 received: ${event.text}")
                is Event.Done -> {
                    println("\t\t\t\tCollector 2 received: Done")
                    cancel()
                }
            }
        }
    }

    // Coletor 3
    val c3 = launch {
        delay(10000)
        println("\t\t\t\t\t\t→ Collector 3 starting")
        sharedFlow.collect { event ->
            when (event) {
                is Event.Msg -> println("\t\t\t\t\t\tCollector 3 received: ${event.text}")
                is Event.Done -> {
                    println("\t\t\t\t\t\tCollector 3 received: Done")
                    cancel()
                }
            }
        }
    }

    // Aguarda que todos os coletores terminem
    joinAll(c1, c2, c3)
    println("All collectors finished. Exiting.")
}
