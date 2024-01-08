package io.novafoundation.nova.feature_staking_impl.data.proxy

import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.intoOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_proxy_api.data.calls.addProxyCall
import io.novafoundation.nova.feature_proxy_api.data.common.ProxyDepositCalculator
import io.novafoundation.nova.feature_staking_api.data.proxy.AddStakingProxyRepository
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealAddStakingProxyRepository(
    private val extrinsicService: ExtrinsicService,
    private val proxyDepositCalculator: ProxyDepositCalculator,
    private val getProxyRepository: GetProxyRepository
) : io.novafoundation.nova.feature_staking_api.data.proxy.AddStakingProxyRepository {

    override suspend fun estimateFee(chain: Chain, accountId: AccountId): Fee {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFee(chain, accountId.intoOrigin()) {
                addProxyCall(accountId, ProxyType.Staking)
            }
        }
    }

    override suspend fun addProxy(chain: Chain, accountId: AccountId): Result<ExtrinsicSubmission> {
        return withContext(Dispatchers.IO) {

            extrinsicService.submitExtrinsic(chain, accountId.intoOrigin()) {
                addProxyCall(accountId, ProxyType.Staking)
            }
        }
    }

    override suspend fun calculateDepositForAddProxy(chain: Chain, accountId: AccountId): Balance {
        val depositConstants = proxyDepositCalculator.getDepositConstants(chain)
        val currentProxiesCount = getProxyRepository.getProxiesQuantity(chain.id, accountId)
        return proxyDepositCalculator.calculateProxyDeposit(depositConstants, currentProxiesCount + 1)
    }
}
