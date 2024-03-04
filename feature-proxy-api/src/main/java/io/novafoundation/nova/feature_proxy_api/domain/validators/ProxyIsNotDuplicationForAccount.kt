package io.novafoundation.nova.feature_proxy_api.domain.validators

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId

class ProxyIsNotDuplicationForAccount<P, E>(
    private val chain: (P) -> Chain,
    private val proxiedAccountId: (P) -> AccountId,
    private val proxyAccountId: (P) -> AccountId,
    private val proxyType: (P) -> ProxyType,
    private val error: (P) -> E,
    private val proxyRepository: GetProxyRepository
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val chain = chain(value)
        val proxyTypes = proxyRepository.getDelegatedProxyTypesLocal(chain.id, proxiedAccountId(value), proxyAccountId(value))

        return validOrError(!proxyTypes.contains(proxyType(value))) {
            error(value)
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.proxyIsNotDuplicationForAccount(
    chain: (P) -> Chain,
    proxiedAccountId: (P) -> AccountId,
    proxyAccountId: (P) -> AccountId,
    proxyType: (P) -> ProxyType,
    error: (P) -> E,
    proxyRepository: GetProxyRepository
) {
    validate(
        ProxyIsNotDuplicationForAccount(
            chain = chain,
            proxiedAccountId = proxiedAccountId,
            proxyAccountId = proxyAccountId,
            proxyType = proxyType,
            error = error,
            proxyRepository = proxyRepository
        )
    )
}
