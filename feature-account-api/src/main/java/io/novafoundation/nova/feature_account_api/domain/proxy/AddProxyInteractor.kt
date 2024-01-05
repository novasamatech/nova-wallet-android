package io.novafoundation.nova.feature_account_api.domain.proxy

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface AddProxyInteractor {

    suspend fun estimateFee(chain: Chain, accountId: AccountId, proxyType: ProxyAccount.ProxyType): Fee

    suspend fun addProxy(chain: Chain, accountId: AccountId, proxyType: ProxyAccount.ProxyType): Result<ExtrinsicSubmission>

    suspend fun calculateDepositForAddProxy(chain: Chain, accountId: AccountId): BigInteger
}
