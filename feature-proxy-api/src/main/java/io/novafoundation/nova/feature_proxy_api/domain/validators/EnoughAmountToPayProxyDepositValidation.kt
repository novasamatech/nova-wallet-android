package io.novafoundation.nova.feature_proxy_api.domain.validators

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class EnoughAmountToPayProxyDepositValidation<P, E>(
    private val chain: (P) -> Chain,
    private val accountId: (P) -> AccountId,
    private val newDeposit: (P) -> BigInteger,
    private val availableBalance: (P) -> BigInteger,
    private val error: (P, BigInteger) -> E,
    private val proxyRepository: GetProxyRepository
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val availableBalance = availableBalance(value)
        val currentDeposit = proxyRepository.getProxyDeposit(chain(value).id, accountId(value))

        val deltaDeposit = (newDeposit(value) - currentDeposit).atLeastZero()

        return validOrError(deltaDeposit <= availableBalance) {
            error(value, availableBalance)
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.enoughBalanceToPayProxyDeposit(
    chain: (P) -> Chain,
    accountId: (P) -> AccountId,
    newDeposit: (P) -> BigInteger,
    availableBalance: (P) -> BigInteger,
    error: (P, BigInteger) -> E,
    proxyRepository: GetProxyRepository
) {
    validate(
        EnoughAmountToPayProxyDepositValidation(
            chain = chain,
            accountId = accountId,
            newDeposit = newDeposit,
            availableBalance = availableBalance,
            error = error,
            proxyRepository = proxyRepository
        )
    )
}
