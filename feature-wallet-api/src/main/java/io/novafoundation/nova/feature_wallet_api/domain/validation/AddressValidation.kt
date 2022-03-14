package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.runtime.ext.isValidAddress
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class AddressValidation<P, E>(
    private val address: (P) -> String,
    private val chain: (P) -> Chain,
    private val error: (P) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        return validOrError(chain(value).isValidAddress(address(value))) {
            error(value)
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.validAddress(
    address: (P) -> String,
    chain: (P) -> Chain,
    error: (P) -> E
) = validate(
    AddressValidation(
        address = address,
        chain = chain,
        error = error
    )
)
