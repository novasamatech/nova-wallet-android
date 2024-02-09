package io.novafoundation.nova.feature_staking_impl.domain.staking.unbond

import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.controllerTransactionOrigin
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.chill
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.unbond
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding
import io.novafoundation.nova.runtime.state.selectedOption
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigInteger

class UnbondInteractor(
    private val extrinsicService: ExtrinsicService,
    private val stakingRepository: StakingRepository,
    private val stakingSharedState: StakingSharedState,
    private val stakingSharedComputation: StakingSharedComputation,
) {

    suspend fun estimateFee(
        stashState: StakingState.Stash,
        currentBondedBalance: BigInteger,
        amount: BigInteger
    ): Fee {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFee(stashState.chain, stashState.controllerTransactionOrigin()) {
                constructUnbondExtrinsic(stashState, currentBondedBalance, amount)
            }
        }
    }

    suspend fun unbond(
        stashState: StakingState.Stash,
        currentBondedBalance: BigInteger,
        amount: BigInteger
    ): Result<ExtrinsicSubmission> {
        return withContext(Dispatchers.IO) {
            extrinsicService.submitExtrinsic(stashState.chain, stashState.controllerTransactionOrigin()) {
                constructUnbondExtrinsic(stashState, currentBondedBalance, amount)
            }
        }
    }

    fun unbondingsFlow(stakingState: StakingState.Stash, sharedComputationScope: CoroutineScope): Flow<Unbondings> {
        return combineToPair(
            stakingRepository.ledgerFlow(stakingState),
            stakingRepository.observeActiveEraIndex(stakingState.chain.id)
        ).flatMapLatest { (ledger, activeEraIndex) ->
            stakingSharedComputation.constructUnbondingList(
                eraRedeemables = ledger.unlocking,
                activeEra = activeEraIndex,
                stakingOption = stakingSharedState.selectedOption(),
                sharedComputationScope = sharedComputationScope
            ).map { unbondings ->
                Unbondings.from(unbondings, rebondPossible = true)
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
