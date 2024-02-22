package io.novafoundation.nova.feature_crowdloan_impl.domain.contributions

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.common.utils.mapResult
import io.novafoundation.nova.core_db.dao.ContributionDao
import io.novafoundation.nova.core_db.dao.DeleteAssetContributionsParams
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.bindContribution
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.data.source.contribution.ExternalContributionSource
import io.novafoundation.nova.feature_crowdloan_api.data.source.contribution.supports
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.mapContributionFromLocal
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.u32
import io.novasama.substrate_sdk_android.runtime.definitions.types.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import java.math.BigInteger

private const val CONTRIBUTIONS_CHILD_SUFFIX = "crowdloan"

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
        fundInfos: Map<ParaId, FundInfo>,
    ): Flow<Pair<String, Result<List<Contribution>>>> = flow {
        if (!chain.hasCrowdloans) {
            return@flow
        }

        val directContributionFlow = directContributionsFlow(chain, chain.utilityAsset, accountId, fundInfos)
            .map { Contribution.DIRECT_SOURCE_ID to it }

        val externalContributionFlows = externalContributionsSources.map { source ->
            externalContributionsFlow(source, chain, chain.utilityAsset, accountId).map { source.sourceId to it }
        }

        val contributionsFlows = externalContributionFlows + listOf(directContributionFlow)

        emitAll(contributionsFlows.merge())
    }

    private fun directContributionsFlow(
        chain: Chain,
        asset: Chain.Asset,
        accountId: ByteArray,
        fundInfos: Map<ParaId, FundInfo>,
    ): Flow<Result<List<Contribution>>> = flowOf {
        runCatching {
            getDirectContributions(chain, asset, accountId, fundInfos)
        }
    }

    override suspend fun getDirectContributions(
        chain: Chain,
        asset: Chain.Asset,
        accountId: ByteArray,
        fundInfos: Map<ParaId, FundInfo>,
    ): List<Contribution> {
        return withContext(Dispatchers.Default) {
            fundInfos.map { (paraId, fundInfo) ->
                async { getDirectContribution(chain, asset, accountId, paraId, fundInfo.trieIndex) }
            }
                .awaitAll()
                .filterNotNull()
        }
    }

    private fun externalContributionsFlow(
        externalContributionSource: ExternalContributionSource,
        chain: Chain,
        asset: Chain.Asset,
        accountId: ByteArray,
    ): Flow<Result<List<Contribution>>> {
        if (externalContributionSource.supports(chain)) {
            return flowOf { externalContributionSource.getContributions(chain, accountId) }
                .mapResult { contributions ->
                    contributions.map {
                        Contribution(
                            chain = chain,
                            asset = asset,
                            amountInPlanks = it.amount,
                            sourceId = it.sourceId,
                            paraId = it.paraId
                        )
                    }
                }
        }

        return emptyFlow()
    }

    override suspend fun getDirectContribution(
        chain: Chain,
        asset: Chain.Asset,
        accountId: ByteArray,
        paraId: ParaId,
        trieIndex: BigInteger,
    ): Contribution? {
        val contribution = remoteStorage.queryChildState(
            storageKeyBuilder = { accountId.toHexString(withPrefix = true) },
            childKeyBuilder = {
                val suffix = (CONTRIBUTIONS_CHILD_SUFFIX.encodeToByteArray() + u32.toByteArray(it, trieIndex))
                    .blake2b256()

                write(suffix)
            },
            binder = { scale, runtime -> scale?.let { bindContribution(it, runtime) } },
            chainId = chain.id
        )

        return contribution?.let {
            Contribution(
                chain = chain,
                asset = asset,
                amountInPlanks = contribution.amount,
                paraId = paraId,
                sourceId = contribution.sourceId,
            )
        }
    }

    override suspend fun deleteContributions(assetIds: List<FullChainAssetId>) {
        val params = assetIds.map { DeleteAssetContributionsParams(it.chainId, it.assetId) }

        contributionDao.deleteAssetContributions(params)
    }
}
