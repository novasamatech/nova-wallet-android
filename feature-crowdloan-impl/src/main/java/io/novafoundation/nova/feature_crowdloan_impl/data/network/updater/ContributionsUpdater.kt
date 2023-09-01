package io.novafoundation.nova.feature_crowdloan_impl.data.network.updater

import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.dao.ContributionDao
import io.novafoundation.nova.core_db.dao.ExternalBalanceDao
import io.novafoundation.nova.core_db.dao.updateExternalBalance
import io.novafoundation.nova.core_db.model.ContributionLocal
import io.novafoundation.nova.core_db.model.ExternalBalanceLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.AssetBalanceScope
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.AssetBalanceScope.ScopeValue
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.ContributionsUpdaterFactory
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.mapContributionToLocal
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach

class RealContributionsUpdaterFactory(
    private val contributionsRepository: ContributionsRepository,
    private val crowdloanRepository: CrowdloanRepository,
    private val contributionDao: ContributionDao,
    private val externalBalanceDao: ExternalBalanceDao,
) : ContributionsUpdaterFactory {

    override fun create(chain: Chain, assetBalanceScope: AssetBalanceScope): Updater<ScopeValue> {
        return ContributionsUpdater(
            assetBalanceScope,
            chain,
            contributionsRepository,
            crowdloanRepository,
            contributionDao,
            externalBalanceDao,
        )
    }
}

class ContributionsUpdater(
    override val scope: AssetBalanceScope,
    private val chain: Chain,
    private val contributionsRepository: ContributionsRepository,
    private val crowdloanRepository: CrowdloanRepository,
    private val contributionDao: ContributionDao,
    private val externalBalanceDao: ExternalBalanceDao,
) : Updater<ScopeValue> {

    override val requiredModules: List<String> = emptyList()

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        scopeValue: ScopeValue,
    ): Flow<Updater.SideEffect> {
        return flowOfAll {
            if (scopeValue.asset.token.configuration.enabled) {
                sync(scopeValue)
            } else {
                emptyFlow()
            }
        }.noSideAffects()
    }

    private suspend fun sync(scopeValue: ScopeValue): Flow<Any> {
        val metaAccount = scopeValue.metaAccount
        val chainAsset = chain.utilityAsset
        val accountId = metaAccount.accountIdIn(chain) ?: return emptyFlow()

        val fundInfos = crowdloanRepository.allFundInfos(chain.id)

        return contributionsRepository.loadContributionsGraduallyFlow(
            chain = chain,
            accountId = accountId,
            fundInfos = fundInfos,
        ).onEach { (sourceId, contributionsResult) ->
            contributionsResult.onSuccess {  contributions ->
                val newContributions = contributions.map { mapContributionToLocal(metaAccount.id, it) }
                val oldContributions = contributionDao.getContributions(metaAccount.id, chain.id, chainAsset.id, sourceId)

                val collectionDiffer = CollectionDiffer.findDiff(newContributions, oldContributions, false)
                contributionDao.updateContributions(collectionDiffer)
                insertExternalBalance(newContributions, sourceId, chainAsset, metaAccount)
            }
        }
    }

    private suspend fun insertExternalBalance(
        contributions: List<ContributionLocal>,
        sourceId: String,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount
    ) {
        val totalSourceContributions = contributions.sumByBigInteger { it.amountInPlanks }

        val externalBalance = ExternalBalanceLocal(
            metaId = metaAccount.id,
            chainId = chain.id,
            assetId = chainAsset.id,
            type = ExternalBalanceLocal.Type.CROWDLOAN,
            subtype = sourceId,
            amount = totalSourceContributions
        )

        externalBalanceDao.updateExternalBalance(externalBalance)
    }
}
