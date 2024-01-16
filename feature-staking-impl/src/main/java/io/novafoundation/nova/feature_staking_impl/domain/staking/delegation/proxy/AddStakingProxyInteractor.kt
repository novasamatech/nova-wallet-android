package io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyDepositWithQuantity
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface AddStakingProxyInteractor {

    suspend fun estimateFee(chain: Chain, proxiedAccountId: AccountId): Fee

    suspend fun addProxy(chain: Chain, proxiedAccountId: AccountId, proxyAccountId: AccountId): Result<ExtrinsicSubmission>

    suspend fun calculateDepositForAddProxy(chain: Chain, accountId: AccountId): ProxyDepositWithQuantity
}
