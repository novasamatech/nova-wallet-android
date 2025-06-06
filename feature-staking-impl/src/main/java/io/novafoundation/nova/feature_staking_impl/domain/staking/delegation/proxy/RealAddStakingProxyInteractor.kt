package io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy

import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.intoOrigin
import io.novafoundation.nova.feature_account_api.data.externalAccounts.ExternalAccountsSyncService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.awaitInBlock
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_proxy_api.data.calls.addProxyCall
import io.novafoundation.nova.feature_proxy_api.data.common.ProxyDepositCalculator
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_api.data.repository.ProxyConstantsRepository
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.emptyAccountId
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealAddStakingProxyInteractor(
    private val extrinsicService: ExtrinsicService,
    private val proxyDepositCalculator: ProxyDepositCalculator,
    private val getProxyRepository: GetProxyRepository,
    private val proxyConstantsRepository: ProxyConstantsRepository,
    private val externalAccountsSyncService: ExternalAccountsSyncService,
) : AddStakingProxyInteractor {

    override suspend fun estimateFee(chain: Chain, proxiedAccountId: AccountId): Fee {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFee(chain, proxiedAccountId.intoOrigin()) {
                addProxyCall(chain.emptyAccountId(), ProxyType.Staking)
            }
        }
    }

    override suspend fun addProxy(chain: Chain, proxiedAccountId: AccountId, proxyAccountId: AccountId): Result<ExtrinsicStatus.InBlock> {
        return withContext(Dispatchers.Default) {
            val result = extrinsicService.submitAndWatchExtrinsic(chain, proxiedAccountId.intoOrigin()) {
                addProxyCall(proxyAccountId, ProxyType.Staking)
            }

            result.awaitInBlock().also { externalAccountsSyncService.sync(chain) }
        }
    }

    override suspend fun calculateDeltaDepositForAddProxy(chain: Chain, accountId: AccountId): Balance {
        val depositConstants = proxyConstantsRepository.getDepositConstants(chain.id)
        val currentProxiesCount = getProxyRepository.getProxiesQuantity(chain.id, accountId)
        val oldDeposit = proxyDepositCalculator.calculateProxyDepositForQuantity(depositConstants, currentProxiesCount)
        val newDeposit = proxyDepositCalculator.calculateProxyDepositForQuantity(depositConstants, currentProxiesCount + 1)
        return newDeposit - oldDeposit
    }
}
