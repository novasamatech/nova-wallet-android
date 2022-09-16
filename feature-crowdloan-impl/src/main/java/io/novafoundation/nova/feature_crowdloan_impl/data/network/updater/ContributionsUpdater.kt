package io.novafoundation.nova.feature_crowdloan_impl.data.network.updater

import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.dao.ContributionDao
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.ContributionsUpdaterFactory
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.mapContributionToLocal
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.mapContributionTypeToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class RealContributionsUpdaterFactory(
    private val scope: AccountUpdateScope,
    private val contributionsRepository: ContributionsRepository,
    private val crowdloanRepository: CrowdloanRepository,
    private val chainStateRepository: ChainStateRepository,
    private val contributionDao: ContributionDao
) : ContributionsUpdaterFactory {

    override fun create(chain: Chain): Updater {
        return ContributionsUpdater(
            scope,
            chain,
            contributionsRepository,
            crowdloanRepository,
            chainStateRepository,
            contributionDao
        )
    }
}

class ContributionsUpdater(
    override val scope: AccountUpdateScope,
    private val chain: Chain,
    private val contributionsRepository: ContributionsRepository,
    private val crowdloanRepository: CrowdloanRepository,
    private val chainStateRepository: ChainStateRepository,
    private val contributionDao: ContributionDao,
) : Updater {

    override val requiredModules: List<String> = emptyList()

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SubscriptionBuilder): Flow<Updater.SideEffect> {
        val metaAccount = scope.getAccount()

        val fundInfos = crowdloanRepository.allFundInfos(chain.id) // can throw exception
        val blocksPerLeasePeriod = crowdloanRepository.leasePeriodToBlocksConverter(chain.id)
        val currentBlockNumber = chainStateRepository.currentBlock(chain.id) // can lock thread
        val expectedBlockTime = chainStateRepository.expectedBlockTimeInMillis(chain.id)

        return contributionsRepository.loadContributionsGraduallyFlow(
            chain,
            metaAccount.accountIdIn(chain)!!,
            fundInfos,
            blocksPerLeasePeriod,
            currentBlockNumber,
            expectedBlockTime
        )
            .onEach { (contributionType, contributions) ->
                val contributionTypeLocal = mapContributionTypeToLocal(contributionType)
                contributionDao.deleteByType(metaAccount.id, chain.id, contributionTypeLocal)

                val contributionsLocal = contributions.map { mapContributionToLocal(metaAccount.id, it) }
                contributionDao.insert(contributionsLocal)
            }
            .noSideAffects()
    }
}
