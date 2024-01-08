package io.novafoundation.nova.feature_staking_api.data.proxy

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface AddStakingProxyRepository {

    suspend fun estimateFee(chain: Chain, accountId: AccountId): Fee

    suspend fun addProxy(chain: Chain, accountId: AccountId): Result<ExtrinsicSubmission>

    suspend fun calculateDepositForAddProxy(chain: Chain, accountId: AccountId): BigInteger
}
