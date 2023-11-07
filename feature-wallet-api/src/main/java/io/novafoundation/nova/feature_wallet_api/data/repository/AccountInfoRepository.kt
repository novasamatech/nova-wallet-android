package io.novafoundation.nova.feature_wallet_api.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface AccountInfoRepository {
    suspend fun saveAccountInfo(chain: Chain, accountInfo: AccountInfo)

    suspend fun getAccountInfo(chain: Chain): AccountInfo

    fun observeAccountInfo(chain: Chain): Flow<AccountInfo>
}
