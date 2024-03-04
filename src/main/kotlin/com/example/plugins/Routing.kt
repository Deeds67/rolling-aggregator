package com.example.plugins
import com.example.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(rollingStatsCalculator: RollingStatsCalculator) {
    install(ContentNegotiation)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
        }
        exception<Errors> { call, cause ->
            call.respondText(text = "${cause.statusCode.value}: ${cause.message}", status = cause.statusCode)
        }
    }
    routing {
        post("/event") {
            val currentTime = System.currentTimeMillis()
            val text = call.receiveText()
            val parsedEvents = Event.fromPlainTextString(text)
            val successfullyParsedEvents = parsedEvents.filterIsInstance<EventParsingResult.Success>().map { it.event }

            // If we didn't get any successful events, we return a 400
            if (successfullyParsedEvents.isEmpty()) {
                throw InvalidEventError(text)
            }
            rollingStatsCalculator.addEventsAndRemoveStaleEvents(successfullyParsedEvents, currentTime)

            // Events that are not successfully parsed are ignored.
            // We could consider returning a 207 here with the status of each event in the payload.
            call.respond(HttpStatusCode.Accepted)
        }
        get("/stats") {
            val currentTime = System.currentTimeMillis()
            val stats = rollingStatsCalculator.getRollingAverageAndRemoveStaleEvents(currentTime)
            call.respondText(
                text = stats.toPlainTextString(),
                contentType = ContentType.Text.Plain
            )
        }
    }
}
