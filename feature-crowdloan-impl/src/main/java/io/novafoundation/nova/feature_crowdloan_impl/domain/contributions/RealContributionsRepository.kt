package io.novafoundation.nova.feature_crowdloan_impl.domain.contributions

import android.util.Log
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.core_db.dao.ContributionDao
import io.novafoundation.nova.core_db.dao.DeleteAssetContributionsParams
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.data.source.contribution.ExternalContributionSource
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution.Companion.DIRECT_SOURCE_ID
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.mapContributionFromLocal
import io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.network.ahOps
import io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.network.rcCrowdloanContribution
import io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.network.rcLeaseReserve
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.queryCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class RealContributionsRepository(
    private val externalContributionsSources: List<ExternalContributionSource>,
    private val chainRegistry: ChainRegistry,
    private val remoteStorage: StorageDataSource,
    private val contributionDao: ContributionDao
) : ContributionsRepository {

    override fun observeContributions(metaAccount: MetaAccount): Flow<List<Contribution>> {
        val contributionsFlow = contributionDao.observeContributions(metaAccount.id)
        val chainsFlow = chainRegistry.chainsById
        return combine(contributionsFlow, chainsFlow) { contributions, chains ->
            contributions.map {
                mapContributionFromLocal(it, chains.getValue(it.chainId))
            }
        }
    }

    override fun observeContributions(metaAccount: MetaAccount, chain: Chain, asset: Chain.Asset): Flow<List<Contribution>> {
        return contributionDao.observeContributions(metaAccount.id, chain.id, asset.id)
            .mapList { mapContributionFromLocal(it, chain) }
    }

    override fun loadContributionsGraduallyFlow(
        chain: Chain,
        accountId: ByteArray,
    ): Flow<Pair<String, Result<List<Contribution>>>> = flow {
        if (!chain.hasCrowdloans) {
            return@flow
        }

        val directContributions = getDirectContributions(chain, chain.utilityAsset, accountId)
            .onFailure { Log.e("RealContributionsRepository", "Failed to fetch direct contributions on ${chain.name}", it) }
        emit(DIRECT_SOURCE_ID to directContributions)
    }

    override suspend fun getDirectContributions(
        chain: Chain,
        asset: Chain.Asset,
        accountId: ByteArray,
    ): Result<List<Contribution>> {
        return withContext(Dispatchers.Default) {
            remoteStorage.queryCatching(chain.id) {
                val reserves = metadata.ahOps.rcLeaseReserve.keys()
                val contributionKeys = reserves.map { (unlockBlock, paraId, _) -> Triple(unlockBlock, paraId, accountId.intoKey()) }
                metadata.ahOps.rcCrowdloanContribution.entries(contributionKeys)
            }.map { contributions ->
                contributions.map { (key, balance) ->
                    val (unlockBlock, paraId) = key
                    Contribution(
                        chain = chain,
                        asset = asset,
                        amountInPlanks = balance,
                        paraId = paraId,
                        sourceId = DIRECT_SOURCE_ID,
                        unlockBlock = unlockBlock
                    )
                }
            }
        }
    }

    override suspend fun deleteContributions(assetIds: List<FullChainAssetId>) {
        val params = assetIds.map { DeleteAssetContributionsParams(it.chainId, it.assetId) }

        contributionDao.deleteAssetContributions(params)
    }
}
