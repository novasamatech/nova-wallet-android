package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.ExternalBalanceDao
import io.novafoundation.nova.core_db.model.AggregatedExternalBalanceLocal
import io.novafoundation.nova.core_db.model.ExternalBalanceLocal
import io.novafoundation.nova.feature_wallet_api.data.repository.ExternalBalanceRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.Flow

internal class RealExternalBalanceRepository(
    private val externalBalanceDao: ExternalBalanceDao,
): ExternalBalanceRepository {

    override fun observeAccountExternalBalances(metaId: Long): Flow<List<ExternalBalance>> {
        return externalBalanceDao.observeAggregatedExternalBalances(metaId).mapList(::mapExternalBalanceFromLocal)
    }

    override fun observeAccountChainExternalBalances(metaId: Long, assetId: FullChainAssetId): Flow<List<ExternalBalance>> {
        return externalBalanceDao.observeChainAggregatedExternalBalances(metaId, assetId.chainId, assetId.assetId)
            .mapList(::mapExternalBalanceFromLocal)
    }

    private fun mapExternalBalanceFromLocal(externalBalance: AggregatedExternalBalanceLocal): ExternalBalance {
        return ExternalBalance(
            chainAssetId = FullChainAssetId(externalBalance.chainId, externalBalance.assetId),
            amount = externalBalance.aggregatedAmount,
            type = mapExternalBalanceTypeFromLocal(externalBalance.type)
        )
    }

    private fun mapExternalBalanceTypeFromLocal(local: ExternalBalanceLocal.Type): ExternalBalance.Type {
        return when(local) {
            ExternalBalanceLocal.Type.CROWDLOAN -> ExternalBalance.Type.CROWDLOAN
            ExternalBalanceLocal.Type.NOMINATION_POOL -> ExternalBalance.Type.NOMINATION_POOL
        }
    }
}
