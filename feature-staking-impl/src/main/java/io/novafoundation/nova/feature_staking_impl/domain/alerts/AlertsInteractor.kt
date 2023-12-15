package io.novafoundation.nova.feature_staking_impl.domain.alerts

import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.data.repository.BagListRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.domain.alerts.Alert.ChangeValidators.Reason
import io.novafoundation.nova.feature_staking_impl.domain.bagList.BagListScoreConverter
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.isWaiting
import io.novafoundation.nova.feature_staking_impl.domain.isActive
import io.novafoundation.nova.feature_staking_impl.domain.isOversubscribed
import io.novafoundation.nova.feature_staking_impl.domain.model.BagListNode
import io.novafoundation.nova.feature_staking_impl.domain.nominationStatus
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import java.math.BigInteger

private const val NOMINATIONS_ACTIVE_MEMO = "NOMINATIONS_ACTIVE_MEMO"

class AlertsInteractor(
    private val stakingRepository: StakingRepository,
    private val stakingConstantsRepository: StakingConstantsRepository,
    private val walletRepository: WalletRepository,
    private val stakingSharedComputation: StakingSharedComputation,
    private val bagListRepository: BagListRepository,
    private val totalIssuanceRepository: TotalIssuanceRepository,
) {

    class AlertContext(
        val exposures: Map<String, Exposure>,
        val stakingState: StakingState,
        val maxRewardedNominatorsPerValidator: Int?,
        val minRecommendedStake: Balance,
        val activeEra: BigInteger,
        val asset: Asset,
        val bagListNode: BagListNode?,
        val bagListScoreConverter: BagListScoreConverter,
    ) {

        val memo = mutableMapOf<Any, Any?>()

        inline fun <reified T> useMemo(
            key: Any,
            lazyProducer: () -> T,
        ): T {
            return memo.getOrPut(key, lazyProducer) as T
        }
    }

    private fun AlertContext.nominationStatus(stashId: AccountId) = useMemo(NOMINATIONS_ACTIVE_MEMO) {
        nominationStatus(stashId, exposures.values, maxRewardedNominatorsPerValidator)
    }
    private fun AlertContext.isStakingActive(stashId: AccountId) = nominationStatus(stashId).isActive

    private fun produceSetValidatorsAlert(context: AlertContext): Alert? {
        return requireState(context.stakingState) { _: StakingState.Stash.None ->
            Alert.SetValidators
        }
    }

    private fun produceChangeValidatorsAlert(context: AlertContext): Alert? {
        return requireState(context.stakingState) { nominatorState: StakingState.Stash.Nominator ->
            val targets = nominatorState.nominations.targets.map { it.toHexString() }

            when {
                // none of nominated validators were elected
                targets.intersect(context.exposures.keys).isEmpty() -> Alert.ChangeValidators(Reason.NONE_ELECTED)

                // user's delegation is elected but it is in oversubscribed part
                context.nominationStatus(nominatorState.stashId).isOversubscribed &&
                    // there is no pending change
                    nominatorState.nominations.isWaiting(context.activeEra).not() -> Alert.ChangeValidators(Reason.OVERSUBSCRIBED)

                else -> null
            }
        }
    }

    private fun produceRedeemableAlert(context: AlertContext): Alert? = requireState(context.stakingState) { _: StakingState.Stash ->
        with(context.asset) {
            if (redeemable > BigDecimal.ZERO) Alert.RedeemTokens(redeemable, token) else null
        }
    }

    private fun produceMinStakeAlert(context: AlertContext) = requireState(context.stakingState) { state: StakingState.Stash ->
        with(context) {
            if (
                // do not show alert for validators
                state !is StakingState.Stash.Validator &&
                asset.bondedInPlanks < context.minRecommendedStake &&
                // prevent alert for situation where all tokens are being unbounded
                asset.bondedInPlanks > BigInteger.ZERO
            ) {
                val minimalStake = asset.token.amountFromPlanks(context.minRecommendedStake)

                Alert.BondMoreTokens(minimalStake, asset.token)
            } else {
                null
            }
        }
    }

    private fun produceWaitingNextEraAlert(context: AlertContext) = requireState(context.stakingState) { nominatorState: StakingState.Stash.Nominator ->
        Alert.WaitingForNextEra.takeIf {
            val isStakingActive = context.isStakingActive(nominatorState.stashId)

            // staking is inactive and there is pending change
            isStakingActive.not() && nominatorState.nominations.isWaiting(context.activeEra)
        }
    }

    private fun produceBagListAlert(context: AlertContext) = requireState(context.stakingState) { _: StakingState.Stash.Nominator ->
        val bagListNode = context.bagListNode ?: return@requireState null

        val currentScore = context.bagListScoreConverter.scoreOf(context.asset.bondedInPlanks)

        Alert.Rebag.takeIf { currentScore > bagListNode.bagUpper }
    }

    private val alertProducers = listOf(
        ::produceChangeValidatorsAlert,
        ::produceRedeemableAlert,
        ::produceMinStakeAlert,
        ::produceWaitingNextEraAlert,
        ::produceSetValidatorsAlert,
        ::produceBagListAlert
    )

    fun getAlertsFlow(stakingState: StakingState, scope: CoroutineScope): Flow<List<Alert>> = flow {
        if (stakingState !is StakingState.Stash) {
            emit(emptyList())
            return@flow
        }

        val chain = stakingState.chain
        val chainAsset = stakingState.chainAsset

        val maxRewardedNominatorsPerValidator = stakingConstantsRepository.maxRewardedNominatorPerValidator(chain.id)
        val totalIssuance = totalIssuanceRepository.getTotalIssuance(chain.id)
        val bagListScoreConverter = BagListScoreConverter.U128(totalIssuance)

        val alertsFlow = combine(
            stakingSharedComputation.activeEraInfo(chain.id, scope),
            walletRepository.assetFlow(stakingState.accountId, chainAsset),
            stakingRepository.observeActiveEraIndex(chain.id),
            bagListRepository.listNodeFlow(stakingState.stashId, chain.id)
        ) { activeEraInfo, asset, activeEra, bagListNode ->

            val context = AlertContext(
                exposures = activeEraInfo.exposures,
                stakingState = stakingState,
                maxRewardedNominatorsPerValidator = maxRewardedNominatorsPerValidator,
                minRecommendedStake = activeEraInfo.minStake,
                asset = asset,
                activeEra = activeEra,
                bagListNode = bagListNode,
                bagListScoreConverter = bagListScoreConverter
            )

            alertProducers.mapNotNull { it.invoke(context) }
        }

        emitAll(alertsFlow)
    }

    private inline fun <reified T : StakingState, R> requireState(
        state: StakingState,
        block: (T) -> R,
    ): R? {
        return (state as? T)?.let(block)
    }
}
