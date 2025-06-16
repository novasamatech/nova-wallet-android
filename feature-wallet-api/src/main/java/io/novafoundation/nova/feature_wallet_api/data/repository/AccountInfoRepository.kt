package io.novafoundation.nova.feature_wallet_api.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId

interface AccountInfoRepository {

    suspend fun getAccountInfo(
        chainId: ChainId,
        accountId: AccountId
    ): AccountInfo
}
