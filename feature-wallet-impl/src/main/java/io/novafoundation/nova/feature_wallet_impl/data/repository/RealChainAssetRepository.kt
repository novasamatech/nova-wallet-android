package io.novafoundation.nova.feature_wallet_impl.data.repository

import com.google.gson.Gson
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainAssetToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class RealChainAssetRepository(
    private val chainAssetDao: ChainAssetDao,
    private val gson: Gson
) : ChainAssetRepository {

    override suspend fun insertCustomAsset(chainAsset: Chain.Asset) {
        val localAsset = mapChainAssetToLocal(chainAsset, gson)
        chainAssetDao.insertAsset(localAsset)
    }
}
