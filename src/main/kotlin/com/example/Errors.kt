package com.example

import io.ktor.http.*

sealed class Errors(override val message: String, val statusCode: HttpStatusCode) : Exception(message)

data class InvalidEventError(val event: String): Errors("The event was invalid.", HttpStatusCode.BadRequest)
data object EmptyPeriodError: Errors("The period was empty.", HttpStatusCode.NotFound)


