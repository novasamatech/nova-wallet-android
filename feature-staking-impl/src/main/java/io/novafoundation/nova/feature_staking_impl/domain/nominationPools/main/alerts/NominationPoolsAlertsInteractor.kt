package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.alerts

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novafoundation.nova.feature_staking_api.domain.model.totalRedeemableIn
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.UnbondingPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.unlockChunksFor
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.bondedAccountOf
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.isWaiting
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.poolState.isPoolStaking
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.alerts.NominationPoolAlert.RedeemTokens
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.alerts.NominationPoolAlert.WaitingForNextEra
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.alerts.RealNominationPoolsAlertsInteractor.AlertsResolutionContext.PoolContext
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface NominationPoolsAlertsInteractor {

    fun alertsFlow(
        poolMember: PoolMember,
        chain: Chain,
        shareComputationScope: CoroutineScope
    ): Flow<List<NominationPoolAlert>>
}

class RealNominationPoolsAlertsInteractor(
    private val nominationPoolsSharedComputation: NominationPoolSharedComputation,
    private val stakingSharedComputation: StakingSharedComputation,
    private val poolAccountDerivation: PoolAccountDerivation,
) : NominationPoolsAlertsInteractor {

    private val alertsConstructors = listOf(
        ::constructWaitingForNextEraAlert,
        ::constructRedeemAlert
    )

    override fun alertsFlow(poolMember: PoolMember, chain: Chain, shareComputationScope: CoroutineScope): Flow<List<NominationPoolAlert>> {
        return flowOfAll {
            val poolId = poolMember.poolId
            val poolStash = poolAccountDerivation.bondedAccountOf(poolId, chain.id)

            combine(
                nominationPoolsSharedComputation.participatingPoolNominationsFlow(poolStash, poolId, chain.id, shareComputationScope),
                nominationPoolsSharedComputation.unbondingPoolsFlow(poolId, chain.id, shareComputationScope),
                stakingSharedComputation.electedExposuresWithActiveEraFlow(chain.id, shareComputationScope),
            ) { poolNominations, unbondingPools, (eraStakers, activeEra) ->
                val alertsContext = AlertsResolutionContext(
                    eraStakers = eraStakers,
                    activeEra = activeEra,
                    pool = PoolContext(
                        nominations = poolNominations,
                        unbonding = unbondingPools,
                        stash = poolStash,
                    ),
                    poolMember = poolMember
                )

                constructAlerts(alertsContext)
            }
        }
    }

    private fun constructAlerts(context: AlertsResolutionContext): List<NominationPoolAlert> {
        return alertsConstructors.mapNotNull { it.invoke(context) }
    }

    private fun constructWaitingForNextEraAlert(context: AlertsResolutionContext): WaitingForNextEra? = with(context) {
        val isPoolStaking = eraStakers.isPoolStaking(pool.stash, pool.nominations)
        val isNominationWaiting = pool.nominations != null && pool.nominations.isWaiting(activeEra)

        val isWaitingForNextEra = !isPoolStaking && isNominationWaiting

        WaitingForNextEra.takeIf { isWaitingForNextEra }
    }

    private fun constructRedeemAlert(context: AlertsResolutionContext): RedeemTokens? = with(context) {
        val unlockChunks = pool.unbonding.unlockChunksFor(poolMember)
        val totalRedeemable = unlockChunks.totalRedeemableIn(activeEra)

        if (totalRedeemable.isPositive()) {
            RedeemTokens(totalRedeemable)
        } else {
            null
        }
    }

    private class AlertsResolutionContext(
        val eraStakers: AccountIdMap<Exposure>,
        val activeEra: EraIndex,
        val pool: PoolContext,
        val poolMember: PoolMember,
    ) {

        class PoolContext(
            val nominations: Nominations?,
            val unbonding: UnbondingPools?,
            val stash: AccountId,
        )
    }
}
