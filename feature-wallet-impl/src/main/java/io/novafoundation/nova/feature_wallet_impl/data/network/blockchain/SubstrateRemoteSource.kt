package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain

import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface SubstrateRemoteSource {

    suspend fun getAccountInfo(
        chainId: ChainId,
        accountId: AccountId
    ): AccountInfo
}
