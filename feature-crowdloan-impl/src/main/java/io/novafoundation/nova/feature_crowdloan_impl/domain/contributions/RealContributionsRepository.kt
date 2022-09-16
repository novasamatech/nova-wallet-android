package io.novafoundation.nova.feature_crowdloan_impl.domain.contributions

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.accumulate
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.hasModule
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.ContributionDao
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.DirectContribution
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.bindContribution
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.LeasePeriodToBlocksConverter
import io.novafoundation.nova.feature_crowdloan_api.data.source.contribution.ExternalContributionSource
import io.novafoundation.nova.feature_crowdloan_api.data.source.contribution.supports
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.mapContributionFromLocal
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.leasePeriodInMillis
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.hash.Hasher.blake2b256
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.primitives.u32
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.toByteArray
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

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

    override fun observeContributions(metaAccount: MetaAccount, chain: Chain): Flow<List<Contribution>> {
        return contributionDao.observeContributions(metaAccount.id, chain.id)
            .mapList { mapContributionFromLocal(it, chain) }
    }

    override fun loadContributionsGraduallyFlow(
        chain: Chain,
        accountId: ByteArray,
        fundInfos: Map<ParaId, FundInfo>,
        blocksPerLeasePeriod: LeasePeriodToBlocksConverter,
        currentBlockNumber: BlockNumber,
        expectedBlockTime: BigInteger
    ): Flow<Pair<Contribution.Type, List<Contribution>>> = flow {

        if (!isCrowdloansAvailable(chain)) {
            return@flow
        }

        val directContributionFlow = directContributionsFlow(
            chain,
            accountId,
            fundInfos,
            blocksPerLeasePeriod,
            currentBlockNumber,
            expectedBlockTime
        ).map {
            Contribution.Type.DIRECT to it
        }

        val externalContributionsFlow = externalContributionsSources.map { source ->
            externalContributionsFlow(
                source,
                chain,
                accountId,
                fundInfos,
                blocksPerLeasePeriod,
                currentBlockNumber,
                expectedBlockTime
            ).map {
                source.contributionsType to it
            }
        }.merge()

        emitAll(merge(directContributionFlow, externalContributionsFlow))
    }

    private fun directContributionsFlow(
        chain: Chain,
        accountId: ByteArray,
        fundInfos: Map<ParaId, FundInfo>,
        blocksPerLeasePeriod: LeasePeriodToBlocksConverter,
        currentBlockNumber: BlockNumber,
        expectedBlockTime: BigInteger
    ): Flow<List<Contribution>> {
        return fundInfos.map { (paraId, fundInfo) ->
            flowOf { getDirectContribution(chain.id, accountId, fundInfo.trieIndex) }
                .mapNotNull { it }
                .map {
                    Contribution(
                        chain = chain,
                        amountInPlanks = it.amount,
                        paraId = paraId,
                        sourceName = null,
                        returnsIn = fundInfo.getDuration(blocksPerLeasePeriod, currentBlockNumber, expectedBlockTime),
                        type = Contribution.Type.DIRECT
                    )
                }
        }.accumulate()
    }

    fun externalContributionsFlow(
        externalContributionSource: ExternalContributionSource,
        chain: Chain,
        accountId: ByteArray,
        fundInfos: Map<ParaId, FundInfo>,
        blocksPerLeasePeriod: LeasePeriodToBlocksConverter,
        currentBlockNumber: BlockNumber,
        expectedBlockTime: BigInteger
    ): Flow<List<Contribution>> {
        if (externalContributionSource.supports(chain)) {
            return flowOf { externalContributionSource.getContributions(chain, accountId) }
                .mapList {
                    val fundInfo = fundInfos.getValue(it.paraId)
                    Contribution(
                        chain = chain,
                        amountInPlanks = it.amount,
                        sourceName = it.sourceName,
                        paraId = it.paraId,
                        returnsIn = fundInfo.getDuration(blocksPerLeasePeriod, currentBlockNumber, expectedBlockTime),
                        type = externalContributionSource.contributionsType
                    )
                }
        }

        return flowOf { emptyList() }
    }


    override suspend fun isCrowdloansAvailable(chain: Chain): Boolean {
        return chainRegistry.getRuntime(chain.id).metadata.hasModule(Modules.CROWDLOAN)
    }

    private suspend fun getDirectContribution(
        chainId: ChainId,
        accountId: ByteArray,
        trieIndex: BigInteger,
    ): DirectContribution? {
        return remoteStorage.queryChildState(
            storageKeyBuilder = { accountId.toHexString(withPrefix = true) },
            childKeyBuilder = {
                val suffix = (CONTRIBUTIONS_CHILD_SUFFIX.encodeToByteArray() + u32.toByteArray(it, trieIndex))
                    .blake2b256()

                write(suffix)
            },
            binder = { scale, runtime -> scale?.let { bindContribution(it, runtime) } },
            chainId = chainId
        )
    }

    private fun FundInfo.getDuration(
        blocksPerLeasePeriod: LeasePeriodToBlocksConverter,
        currentBlockNumber: BlockNumber,
        expectedBlockTime: BigInteger
    ): TimerValue {
        val millis = leasePeriodInMillis(
            leasePeriodToBlocksConverter = blocksPerLeasePeriod,
            currentBlockNumber = currentBlockNumber,
            endingLeasePeriod = lastSlot,
            expectedBlockTimeInMillis = expectedBlockTime,
        )

        return TimerValue(millis, millisCalculatedAt = System.currentTimeMillis())
    }
}
