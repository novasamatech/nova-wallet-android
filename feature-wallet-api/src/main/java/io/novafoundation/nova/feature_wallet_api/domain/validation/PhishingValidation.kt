package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isFalseOrWarning
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class PhishingValidationFactory(
    private val walletRepository: WalletRepository,
) {

    fun <P, E> create(
        address: (P) -> String,
        chain: (P) -> Chain,
        warning: (address: String) -> E
    ) = PhishingValidation(
        walletRepository = walletRepository,
        address = address,
        chain = chain,
        waring = warning
    )
}

class PhishingValidation<P, E>(
    private val walletRepository: WalletRepository,
    private val address: (P) -> String,
    private val chain: (P) -> Chain,
    private val waring: (address: String) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val addressValue = address(value)
        val accountId = chain(value).accountIdOf(addressValue)

        val isPhishingAddress = walletRepository.isAccountIdFromPhishingList(accountId)

        return isPhishingAddress isFalseOrWarning { waring(addressValue) }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.notPhishingAccount(
    factory: PhishingValidationFactory,
    address: (P) -> String,
    chain: (P) -> Chain,
    warning: (address: String) -> E
) = validate(
    factory.create(address, chain, warning)
)
