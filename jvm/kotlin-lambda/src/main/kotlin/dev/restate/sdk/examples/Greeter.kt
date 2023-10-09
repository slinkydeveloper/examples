package dev.restate.sdk.examples

import dev.restate.sdk.core.StateKey
import dev.restate.sdk.examples.generated.*
import dev.restate.sdk.examples.generated.GreeterProto.GreetRequest
import dev.restate.sdk.examples.generated.GreeterProto.GreetResponse
import dev.restate.sdk.kotlin.RestateCoroutineService
import kotlin.coroutines.CoroutineContext

class Greeter(coroutineContext: CoroutineContext) :
        GreeterGrpcKt.GreeterCoroutineImplBase(coroutineContext),
    RestateCoroutineService {

  private val COUNT = StateKey.of("count", Int::class.java)

  override suspend fun greet(request: GreetRequest): GreetResponse {
    val ctx = restateContext()

    val count = ctx.get(COUNT) ?: 1
    ctx.set(COUNT, count + 1)

    return greetResponse {
      message = "Hello ${request.name} for the $count time!"
    }
  }
}
