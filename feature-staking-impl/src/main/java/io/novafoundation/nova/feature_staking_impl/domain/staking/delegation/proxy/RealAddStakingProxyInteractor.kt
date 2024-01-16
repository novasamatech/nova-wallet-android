package io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy

import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.intoOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_proxy_api.data.calls.addProxyCall
import io.novafoundation.nova.feature_proxy_api.data.common.ProxyDepositCalculator
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyDepositWithQuantity
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.ext.emptyAccountId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealAddStakingProxyInteractor(
    private val extrinsicService: ExtrinsicService,
    private val proxyDepositCalculator: ProxyDepositCalculator,
    private val getProxyRepository: GetProxyRepository
) : AddStakingProxyInteractor {

    override suspend fun estimateFee(chain: Chain, proxiedAccountId: AccountId): Fee {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFee(chain, proxiedAccountId.intoOrigin()) {
                addProxyCall(chain.emptyAccountId(), ProxyType.Staking)
            }
        }
    }

    override suspend fun addProxy(chain: Chain, proxiedAccountId: AccountId, proxyAccountId: AccountId): Result<ExtrinsicSubmission> {
        return withContext(Dispatchers.IO) {
            extrinsicService.submitExtrinsic(chain, proxiedAccountId.intoOrigin()) {
                addProxyCall(proxyAccountId, ProxyType.Staking)
            }
        }
    }

    override suspend fun calculateDepositForAddProxy(chain: Chain, accountId: AccountId): ProxyDepositWithQuantity {
        val depositConstants = proxyDepositCalculator.getDepositConstants(chain)
        val currentProxiesCount = getProxyRepository.getProxiesQuantity(chain.id, accountId)
        val newQuantity = currentProxiesCount + 1
        val deposit = proxyDepositCalculator.calculateProxyDepositForQuantity(depositConstants, newQuantity)
        return ProxyDepositWithQuantity(deposit, newQuantity)
    }
}
