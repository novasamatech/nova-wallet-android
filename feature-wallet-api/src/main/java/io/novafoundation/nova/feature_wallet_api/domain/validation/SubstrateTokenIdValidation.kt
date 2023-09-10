package io.novafoundation.nova.feature_wallet_api.domain.validation

import androidx.core.text.isDigitsOnly
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError

class SubstrateTokenIdValidation<P, E>(
    private val tokenId: (P) -> String,
    private val error: (P) -> E,
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val isValidTokenId = tokenId(value).isDigitsOnly()
        return if (isValidTokenId) {
            valid()
        } else {
            validationError(error(value))
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.validSubstrateTokenId(
    tokenId: (P) -> String,
    error: (P) -> E,
) = validate(
    SubstrateTokenIdValidation(
        tokenId = tokenId,
        error = error
    )
)
