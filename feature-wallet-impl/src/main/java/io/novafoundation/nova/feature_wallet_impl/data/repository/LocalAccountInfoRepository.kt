package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.AccountData
import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.core_db.dao.AccountInfoDao
import io.novafoundation.nova.core_db.model.AccountInfoDataLocal
import io.novafoundation.nova.core_db.model.AccountInfoLocal
import io.novafoundation.nova.feature_wallet_api.data.repository.AccountInfoRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class LocalAccountInfoRepository(private val accountInfoDao: AccountInfoDao) : AccountInfoRepository {

    override suspend fun saveAccountInfo(chain: Chain, accountInfo: AccountInfo) {
        val localAccountInfo = mapAccountInfoToLocal(chain, accountInfo)
        accountInfoDao.insertAccountInfo(localAccountInfo)
    }

    override suspend fun getAccountInfo(chain: Chain): AccountInfo {
        val accountInfoLocal = accountInfoDao.getAccountInfo(chain.id)
        return mapAccountInfoFromLocal(accountInfoLocal)
    }

    override fun observeAccountInfo(chain: Chain): Flow<AccountInfo> {
        return accountInfoDao.observeAccountInfo(chain.id)
            .map { mapAccountInfoFromLocal(it) }
    }

    private fun mapAccountInfoToLocal(chain: Chain, accountInfo: AccountInfo): AccountInfoLocal {
        return AccountInfoLocal(
            chainId = chain.id,
            consumers = accountInfo.consumers,
            providers = accountInfo.providers,
            sufficients = accountInfo.sufficients,
            data = AccountInfoDataLocal(
                free = accountInfo.data.free,
                reserved = accountInfo.data.reserved,
                frozen = accountInfo.data.frozen
            )
        )
    }

    private fun mapAccountInfoFromLocal(accountInfo: AccountInfoLocal): AccountInfo {
        return AccountInfo(
            consumers = accountInfo.consumers,
            providers = accountInfo.providers,
            sufficients = accountInfo.sufficients,
            data = AccountData(
                free = accountInfo.data.free,
                reserved = accountInfo.data.reserved,
                frozen = accountInfo.data.frozen
            )
        )
    }
}
