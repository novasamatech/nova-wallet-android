package io.novafoundation.nova.feature_crowdloan_impl.data.network.updater

import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.dao.ContributionDao
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.AssetBalanceScope
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.ContributionsUpdaterFactory
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.mapContributionToLocal
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach

class RealContributionsUpdaterFactory(
    private val accountScope: AccountUpdateScope,
    private val contributionsRepository: ContributionsRepository,
    private val crowdloanRepository: CrowdloanRepository,
    private val contributionDao: ContributionDao
) : ContributionsUpdaterFactory {

    override fun create(chain: Chain, assetBalanceScope: AssetBalanceScope): Updater {
        return ContributionsUpdater(
            assetBalanceScope,
            accountScope,
            chain,
            contributionsRepository,
            crowdloanRepository,
            contributionDao
        )
    }
}

class ContributionsUpdater(
    override val scope: AssetBalanceScope,
    private val accountScope: AccountUpdateScope,
    private val chain: Chain,
    private val contributionsRepository: ContributionsRepository,
    private val crowdloanRepository: CrowdloanRepository,
    private val contributionDao: ContributionDao,
) : Updater {

    override val requiredModules: List<String> = emptyList()

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SharedRequestsBuilder): Flow<Updater.SideEffect> {
        return scope.invalidationFlow().flatMapLatest {
            if (it.token.configuration.enabled) {
                sync()
            } else {
                deleteContributions(it.token.configuration)
            }
        }.noSideAffects()
    }

    private suspend fun sync(): Flow<Any> {
        val metaAccount = accountScope.getAccount()
        val accountId = metaAccount.accountIdIn(chain) ?: return emptyFlow()

        val fundInfos = crowdloanRepository.allFundInfos(chain.id)

        return contributionsRepository.loadContributionsGraduallyFlow(
            chain = chain,
            accountId = accountId,
            fundInfos = fundInfos,
        ).onEach { (sourceId, contributions) ->
            val newContributions = contributions.map { mapContributionToLocal(metaAccount.id, it) }
            val oldContributions = contributionDao.getContributions(metaAccount.id, chain.id, chain.utilityAsset.id, sourceId)
            val collectionDiffer = CollectionDiffer.findDiff(newContributions, oldContributions, false)
            contributionDao.updateContributions(collectionDiffer)
        }
    }

    private fun deleteContributions(asset: Chain.Asset): Flow<*> {
        return flowOf {
            contributionDao.deleteContributions(asset.chainId, asset.id)
        }
    }
}
