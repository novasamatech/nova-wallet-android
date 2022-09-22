package io.novafoundation.nova.feature_crowdloan_impl.data.network.updater

import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.core.updater.UpdateScope
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.dao.ContributionDao
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.ContributionsUpdaterFactory
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.mapContributionToLocal
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach

class RealContributionsUpdaterFactory(
    private val accountScope: AccountUpdateScope,
    private val contributionsRepository: ContributionsRepository,
    private val crowdloanRepository: CrowdloanRepository,
    private val contributionDao: ContributionDao
) : ContributionsUpdaterFactory {

    override fun create(chain: Chain, invalidationScope: UpdateScope): Updater {
        return ContributionsUpdater(
            invalidationScope,
            accountScope,
            chain,
            contributionsRepository,
            crowdloanRepository,
            contributionDao
        )
    }
}

class ContributionsUpdater(
    override val scope: UpdateScope,
    private val accountScope: AccountUpdateScope,
    private val chain: Chain,
    private val contributionsRepository: ContributionsRepository,
    private val crowdloanRepository: CrowdloanRepository,
    private val contributionDao: ContributionDao,
) : Updater {

    override val requiredModules: List<String> = emptyList()

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SubscriptionBuilder): Flow<Updater.SideEffect> {
        return scope.invalidationFlow().flatMapLatest {
            val metaAccount = accountScope.getAccount()

            val fundInfos = crowdloanRepository.allFundInfos(chain.id)

            contributionsRepository.loadContributionsGraduallyFlow(
                chain,
                metaAccount.accountIdIn(chain)!!,
                fundInfos,
            ).onEach { (sourceId, contributions) ->
                val newContributions = contributions.map { mapContributionToLocal(metaAccount.id, it) }
                val oldContributions = contributionDao.getContributions(metaAccount.id, chain.id, chain.utilityAsset.id, sourceId)
                val collectionDiffer = CollectionDiffer.findDiff(newContributions, oldContributions, false)
                contributionDao.updateContributions(collectionDiffer)
            }
        }.noSideAffects()
    }
}
