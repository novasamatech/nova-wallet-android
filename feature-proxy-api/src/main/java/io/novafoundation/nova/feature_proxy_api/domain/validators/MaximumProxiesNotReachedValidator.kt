package io.novafoundation.nova.feature_proxy_api.domain.validators

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class MaximumProxiesNotReachedValidator<P, E>(
    private val chain: (P) -> Chain,
    private val accountId: (P) -> AccountId,
    private val newProxiedQuantity: (P) -> Int,
    private val error: (P, Int) -> E,
    private val proxyRepository: GetProxyRepository
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val newProxiesQuantity = newProxiedQuantity(value)
        val maximumProxiesQuantiy = proxyRepository.getProxiesQuantity(chain(value).id, accountId(value))

        return validOrError(newProxiesQuantity <= newProxiesQuantity) {
            error(value, maximumProxiesQuantiy)
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.maximumProxiesNotReached(
    chain: (P) -> Chain,
    accountId: (P) -> AccountId,
    newProxiedQuantity: (P) -> Int,
    error: (P, Int) -> E,
    proxyRepository: GetProxyRepository
) {
    validate(
        MaximumProxiesNotReachedValidator(
            chain = chain,
            accountId = accountId,
            newProxiedQuantity = newProxiedQuantity,
            error = error,
            proxyRepository = proxyRepository
        )
    )
}
