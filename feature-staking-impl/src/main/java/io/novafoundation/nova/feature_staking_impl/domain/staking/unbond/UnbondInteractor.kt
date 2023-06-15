package io.novafoundation.nova.feature_staking_impl.domain.staking.unbond

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.isRedeemableIn
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.chill
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.unbond
import io.novafoundation.nova.feature_staking_impl.domain.common.EraTimeCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding
import io.novafoundation.nova.runtime.state.selectedOption
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.math.BigInteger

class UnbondInteractor(
    private val extrinsicService: ExtrinsicService,
    private val stakingRepository: StakingRepository,
    private val eraTimeCalculator: EraTimeCalculatorFactory,
    private val stakingSharedState: StakingSharedState,
) {

    suspend fun estimateFee(
        stashState: StakingState.Stash,
        currentBondedBalance: BigInteger,
        amount: BigInteger
    ): BigInteger {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFee(stashState.chain) {
                constructUnbondExtrinsic(stashState, currentBondedBalance, amount)
            }
        }
    }

    suspend fun unbond(
        stashState: StakingState.Stash,
        currentBondedBalance: BigInteger,
        amount: BigInteger
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            extrinsicService.submitExtrinsicWithAnySuitableWallet(stashState.chain, stashState.controllerId) {
                constructUnbondExtrinsic(stashState, currentBondedBalance, amount)
            }
        }
    }

    fun unbondingsFlow(stakingState: StakingState.Stash): Flow<Unbondings> {
        return flowOfAll {
            val calculator = eraTimeCalculator.create(stakingSharedState.selectedOption())

            combine(
                stakingRepository.ledgerFlow(stakingState),
                stakingRepository.observeActiveEraIndex(stakingState.chain.id)
            ) { ledger, activeEraIndex ->
                val unbondings = ledger.unlocking.mapIndexed { index, unbonding ->
                    val progressState = if (unbonding.isRedeemableIn(activeEraIndex)) {
                        Unbonding.Status.Redeemable
                    } else {
                        val leftTime = calculator.calculate(destinationEra = unbonding.era)

                        Unbonding.Status.Unbonding(
                            timeLeft = leftTime.toLong(),
                            calculatedAt = System.currentTimeMillis()
                        )
                    }

                    Unbonding(
                        id = "$index:${unbonding.era}:${unbonding.amount}",
                        amount = unbonding.amount,
                        status = progressState,
                    )
                }

                Unbondings.from(unbondings)
            }
        }
    }

    private suspend fun ExtrinsicBuilder.constructUnbondExtrinsic(
        stashState: StakingState.Stash,
        currentBondedBalance: BigInteger,
        unbondAmount: BigInteger
    ) {
        // see https://github.com/paritytech/substrate/blob/master/frame/staking/src/lib.rs#L1614
        if (
            // if account is nominating
            stashState is StakingState.Stash.Nominator &&
            // and resulting bonded balance is less than min bond
            currentBondedBalance - unbondAmount < stakingRepository.minimumNominatorBond(stashState.chain.id)
        ) {
            chill()
        }

        unbond(unbondAmount)
    }

    // unbondings are always going from the oldest to newest so last in the list will be the newest one
    fun newestUnbondingAmount(unbondings: List<Unbonding>) = unbondings.filterNonRedeemable().last().amount

    fun allUnbondingsAmount(unbondings: List<Unbonding>): BigInteger = unbondings.filterNonRedeemable().sumByBigInteger(Unbonding::amount)

    private fun List<Unbonding>.filterNonRedeemable() = filter { it.status is Unbonding.Status.Unbonding }
}
