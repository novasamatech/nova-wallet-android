package io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.remove

import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.intoOrigin
import io.novafoundation.nova.feature_account_api.data.externalAccounts.ExternalAccountsSyncService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.awaitInBlock
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.ExtrinsicWatchResult
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_proxy_api.data.calls.removeProxyCall
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.ext.emptyAccountId
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface RemoveStakingProxyInteractor {

    suspend fun estimateFee(chain: Chain, proxiedAccountId: AccountId): Fee

    suspend fun removeProxy(chain: Chain, proxiedAccountId: AccountId, proxyAccountId: AccountId): Result<ExtrinsicWatchResult<ExtrinsicStatus.InBlock>>
}

class RealRemoveStakingProxyInteractor(
    private val extrinsicService: ExtrinsicService,
    private val externalAccountsSyncService: ExternalAccountsSyncService,
) : RemoveStakingProxyInteractor {

    override suspend fun estimateFee(chain: Chain, proxiedAccountId: AccountId): Fee {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFee(chain, proxiedAccountId.intoOrigin()) {
                removeProxyCall(chain.emptyAccountId(), ProxyType.Staking)
            }
        }
    }

    override suspend fun removeProxy(
        chain: Chain,
        proxiedAccountId: AccountId,
        proxyAccountId: AccountId
    ): Result<ExtrinsicWatchResult<ExtrinsicStatus.InBlock>> {
        return withContext(Dispatchers.Default) {
            val result = extrinsicService.submitAndWatchExtrinsic(chain, proxiedAccountId.intoOrigin()) {
                removeProxyCall(proxyAccountId, ProxyType.Staking)
            }

            result.awaitInBlock().also { externalAccountsSyncService.sync() }
        }
    }
}
