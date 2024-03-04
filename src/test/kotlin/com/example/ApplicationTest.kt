package com.example

import com.example.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {
    @Test
    fun `POST event and then GET stats should return 202 and then 200 with a text body`() = testApplication {
        application {
            val rollingStatsCalculator = RollingStatsCalculator()
            configureRouting(rollingStatsCalculator)
        }
        val currentTime = System.currentTimeMillis()

        client.post("/event") {
            header(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            setBody("${currentTime},1.0,1")
        }.apply {
            assertEquals(HttpStatusCode.Accepted, this.status)
        }

        client.get("/stats").apply {
            assertEquals(HttpStatusCode.OK, this.status)
            assertEquals("1,1.0000000000,1.0000000000,1,1.000", this.bodyAsText())
        }
    }

    @Test
    fun `POST event with invalid body should return 400`() = testApplication {
        application {
            val rollingStatsCalculator = RollingStatsCalculator()
            configureRouting(rollingStatsCalculator)
        }
        val currentTime = System.currentTimeMillis()

        client.post("/event") {
            header(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            setBody("${currentTime},a,1")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, this.status)
        }
    }

    @Test
    fun `POST event with some valid and some invalid events in the body should return 202`() = testApplication {
        application {
            val rollingStatsCalculator = RollingStatsCalculator()
            configureRouting(rollingStatsCalculator)
        }
        val currentTime = System.currentTimeMillis()

        client.post("/event") {
            header(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            setBody("${currentTime},1,1" +
                    "${currentTime},a,2")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, this.status)
        }
    }

    @Test
    fun `GET stats when there is no available stats should return 404`() = testApplication {
        application {
            val rollingStatsCalculator = RollingStatsCalculator()
            configureRouting(rollingStatsCalculator)
        }

        client.get("/stats").apply {
            assertEquals(HttpStatusCode.NotFound, this.status)
        }
    }
}
