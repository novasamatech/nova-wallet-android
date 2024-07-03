package io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import java.lang.Exception
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

class NetworkNodeIsAliveValidation<P, F>(
    private val nodeHealthStateCheckRequest: suspend (P) -> Unit,
    private val failure: (P) -> F
) : Validation<P, F> {

    override suspend fun validate(value: P): ValidationStatus<F> {
        return try {
            withTimeout(10.seconds) { nodeHealthStateCheckRequest(value) }

            valid()
        } catch (e: Exception) {
            validationError(failure(value))
        }
    }
}

fun <P, F> ValidationSystemBuilder<P, F>.validateNetworkNodeIsAlive(
    nodeHealthStateCheckRequest: suspend (P) -> Unit,
    failure: (P) -> F
) = validate(
    NetworkNodeIsAliveValidation(nodeHealthStateCheckRequest, failure)
)
