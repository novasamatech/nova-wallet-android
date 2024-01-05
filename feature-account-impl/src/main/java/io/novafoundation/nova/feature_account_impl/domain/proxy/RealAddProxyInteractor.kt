package io.novafoundation.nova.feature_account_impl.domain.proxy

import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.intoOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.repository.ProxyRepository
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.feature_account_api.domain.proxy.AddProxyInteractor
import io.novafoundation.nova.feature_account_impl.data.network.blockchain.bindings.calls.addProxyCall
import io.novafoundation.nova.feature_account_impl.domain.proxy.common.ProxyDepositCalculator
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealAddProxyInteractor(
    private val extrinsicService: ExtrinsicService,
    private val proxyDepositCalculator: ProxyDepositCalculator,
    private val proxyRepository: ProxyRepository
) : AddProxyInteractor {

    override suspend fun estimateFee(chain: Chain, accountId: AccountId, proxyType: ProxyAccount.ProxyType): Fee {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFee(chain, accountId.intoOrigin()) {
                addProxyCall(accountId, proxyType)
            }
        }
    }

    override suspend fun addProxy(chain: Chain, accountId: AccountId, proxyType: ProxyAccount.ProxyType): Result<ExtrinsicSubmission> {
        return withContext(Dispatchers.IO) {

            extrinsicService.submitExtrinsic(chain, accountId.intoOrigin()) {
                addProxyCall(accountId, proxyType)
            }
        }
    }

    override suspend fun calculateDepositForAddProxy(chain: Chain, accountId: AccountId): Balance {
        val depositConstants = proxyDepositCalculator.getDepositConstants(chain)
        val currentProxiesCount = proxyRepository.getProxiesQuantity(chain.id, accountId)
        return proxyDepositCalculator.calculateProxyDeposit(depositConstants, currentProxiesCount + 1)
    }
}
