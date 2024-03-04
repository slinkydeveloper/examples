package dev.restate.patterns

import dev.restate.patterns.compensations.generated.*
import dev.restate.patterns.compensations.generated.Proto.TravelBookingRequest
import dev.restate.sdk.common.TerminalException
import dev.restate.sdk.http.vertx.RestateHttpEndpointBuilder
import dev.restate.sdk.kotlin.KeyedContext

class Compensations {

    class TravelService : TravelRestateKt.TravelRestateKtImplBase() {

        suspend fun reserve(ctx: KeyedContext, request: TravelBookingRequest) {
            val flightsService = FlightsRestateKt.newClient(ctx)
            val paymentService = PaymentRestateKt.newClient(ctx)

            // Create a list of compensations to run in case of a failure or cancellation.
            val compensations: MutableList<suspend () -> Unit> = mutableListOf()

            try {
                val flightBookingId = flightsService
                    .reserve(flightBookingRequest { tripId = request.tripID })
                    .await()
                // Register the compensation to undo the flight reservation.
                compensations.add { flightsService.cancel(flightBookingId).await() }

                val paymentId = paymentService
                    .process(paymentRequest { tripId = request.tripID })
                    .await()
                // Register the compensation to undo the payment.
                compensations.add { paymentService.refund(paymentId).await() }

                // Confirm flight after payment done
                flightsService.confirm(flightBookingId).await()
            } catch (e: TerminalException) {
                // Run the compensations in reverse order
                compensations.reversed().forEach {
                    it()
                }

                throw TerminalException("Failed to reserve the trip: ${e.message}")
            }
        }
    }
}

fun main() {
    RestateHttpEndpointBuilder.builder()
            .withService(Compensations.TravelService())
            .buildAndListen()
}