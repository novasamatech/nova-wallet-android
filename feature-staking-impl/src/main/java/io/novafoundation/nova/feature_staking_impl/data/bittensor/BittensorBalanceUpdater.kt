package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.dao.ExternalBalanceDao
import io.novafoundation.nova.core_db.dao.updateExternalBalance
import io.novafoundation.nova.core_db.model.ExternalBalanceLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_api.data.network.blockhain.updaters.BittensorBalanceUpdaterFactory
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioValuation
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

class RealBittensorBalanceUpdaterFactory(
    private val scope: AccountUpdateScope,
    private val positionRepository: BittensorPositionRepository,
    private val externalBalanceDao: ExternalBalanceDao
) : BittensorBalanceUpdaterFactory {

    override fun create(chain: Chain): Updater<MetaAccount> {
        return BittensorBalanceUpdater(scope, chain, positionRepository, externalBalanceDao)
    }
}

class BittensorBalanceUpdater(
    override val scope: AccountUpdateScope,
    private val chain: Chain,
    private val positionRepository: BittensorPositionRepository,
    private val externalBalanceDao: ExternalBalanceDao
) : Updater<MetaAccount> {

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        scopeValue: MetaAccount
    ): Flow<Updater.SideEffect> {
        if (chain.id != ChainGeneses.BITTENSOR) return emptyFlow()
        val coldKey = scopeValue.accountIdIn(chain) ?: return emptyFlow()

        return positionRepository.observePortfolio(chain.id, coldKey)
            .mapNotNull { state ->
                state.positions?.valuation.takeIf { state.status is BittensorDataStatus.Fresh }
            }
            .distinctUntilChanged()
            .onEach { valuation -> persistValuation(scopeValue.id, valuation) }
            .noSideAffects()
    }

    private suspend fun persistValuation(metaId: Long, valuation: BittensorPortfolioValuation) {
        val assetId = chain.utilityAsset.id
        val type = ExternalBalanceLocal.Type.BITTENSOR
        externalBalanceDao.runInTransaction {
            if (!valuation.isComplete) {
                val complete = externalBalanceDao.getExternalBalance(metaId, chain.id, assetId, type, ExternalBalanceLocal.EMPTY_SUBTYPE)
                if (complete != null) return@runInTransaction
            } else {
                externalBalanceDao.removeExternalBalance(metaId, chain.id, assetId, type, PARTIAL_SUBTYPE)
            }

            val externalBalance = ExternalBalanceLocal(
                metaId = metaId,
                chainId = chain.id,
                assetId = assetId,
                type = type,
                subtype = if (valuation.isComplete) ExternalBalanceLocal.EMPTY_SUBTYPE else PARTIAL_SUBTYPE,
                amount = valuation.amount
            )
            externalBalanceDao.updateExternalBalance(externalBalance)
        }
    }

    private companion object {
        const val PARTIAL_SUBTYPE = "partial"
    }
}
