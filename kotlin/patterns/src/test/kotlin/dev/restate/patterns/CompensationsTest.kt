package dev.restate.patterns

import dev.restate.patterns.Compensations.TravelService
import dev.restate.patterns.compensations.generated.*
import dev.restate.patterns.compensations.generated.Proto.*
import dev.restate.sdk.common.TerminalException
import dev.restate.sdk.kotlin.KeyedContext
import dev.restate.sdk.testing.RestateGrpcChannel
import dev.restate.sdk.testing.RestateRunner
import dev.restate.sdk.testing.RestateRunnerBuilder
import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class CompensationsTest {
    @Test
    fun testGreet( // Channel to send requests to Restate services
            @RestateGrpcChannel channel: ManagedChannel?) {
        val client: TravelGrpc.TravelBlockingStub = TravelGrpc.newBlockingStub(channel)

        Assertions.assertThrows(
                StatusRuntimeException::class.java,
                { client.reserve(travelBookingRequest { this.tripID = "myTrip" }) },
                "INTERNAL: Failed to reserve the trip: Failed to reserve car rental. Ran 1 compensations.")
    }

    // --------------------------------------------------------------------------------------
    // Helper services for the test
    // --------------------------------------------------------------------------------------
    private class MockFlightsService : FlightsRestateKt.FlightsRestateKtImplBase() {

        override suspend fun reserve(context: KeyedContext, request: FlightBookingRequest): FlightBookingId {
            return flightBookingId {
                this.tripId = tripId
            }
        }
    }

    private class MockCarRentalService : CarRentalRestateKt.CarRentalRestateKtImplBase() {
        override suspend fun reserve(context: KeyedContext, request: CarRentalRequest): CarRentalId {
            // let's simulate that the car rental service failed to reserve the car
            throw TerminalException(TerminalException.Code.INTERNAL, "Failed to reserve car rental")
        }
    }

    private class MockPaymentService : PaymentRestateKt.PaymentRestateKtImplBase() {
        override suspend fun process(context: KeyedContext, request: PaymentRequest): PaymentId {
            return paymentId { this.paymentId = request.tripId }
        }
    }

    companion object {
        // Runner runs Restate using testcontainers and registers services
        @RegisterExtension
        private val restateRunner: RestateRunner = RestateRunnerBuilder.create() // Service to test
                .withService(TravelService()) // Helper services
                .withService(MockFlightsService())
                .withService(MockCarRentalService())
                .withService(MockPaymentService())
                .buildRunner()
    }
}
