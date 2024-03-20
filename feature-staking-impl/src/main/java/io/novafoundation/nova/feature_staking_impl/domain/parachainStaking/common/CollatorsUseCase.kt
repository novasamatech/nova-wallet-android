package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.systemForcedMinStake
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.SelectedCollator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.collators.collatorAddressModel
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.selectedOption
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

enum class SelectedCollatorSorting {
    DELEGATION, APR
}

interface CollatorsUseCase {

    suspend fun collatorAddressModel(collator: Collator): AddressModel

    suspend fun getCollator(collatorId: AccountId): Collator

    suspend fun getSelectedCollators(
        delegatorState: DelegatorState,
        sorting: SelectedCollatorSorting = SelectedCollatorSorting.DELEGATION
    ): List<SelectedCollator>

    suspend fun maxRewardedDelegatorsPerCollator(): Int

    suspend fun defaultMinimumStake(): BigInteger
}

class RealCollatorsUseCase(
    private val stakingSharedState: StakingSharedState,
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
    private val collatorProvider: CollatorProvider,
    private val addressIconGenerator: AddressIconGenerator,
) : CollatorsUseCase {

    override suspend fun maxRewardedDelegatorsPerCollator(): Int {
        val chainId = stakingSharedState.chainId()

        return parachainStakingConstantsRepository.maxRewardedDelegatorsPerCollator(chainId).toInt()
    }

    override suspend fun defaultMinimumStake(): BigInteger {
        return parachainStakingConstantsRepository.systemForcedMinStake(stakingSharedState.chainId())
    }

    override suspend fun collatorAddressModel(collator: Collator): AddressModel {
        return addressIconGenerator.collatorAddressModel(
            collator = collator,
            chain = stakingSharedState.chain()
        )
    }

    override suspend fun getCollator(collatorId: AccountId): Collator = withContext(Dispatchers.IO) {
        collatorProvider.getCollator(stakingSharedState.selectedOption(), collatorId)
    }

    override suspend fun getSelectedCollators(
        delegatorState: DelegatorState,
        sorting: SelectedCollatorSorting
    ): List<SelectedCollator> {
        val stakingOption = stakingSharedState.selectedOption()

        return when (delegatorState) {
            is DelegatorState.Delegator -> {
                val delegationAmountByCollator = delegatorState.delegations.associateBy(
                    keySelector = { it.owner.toHexString() },
                    valueTransform = { it.balance }
                )
                val stakedCollatorsIds = delegationAmountByCollator.keys

                val collatorSource = CollatorProvider.CollatorSource.Custom(stakedCollatorsIds)

                collatorProvider.getCollators(stakingOption, collatorSource)
                    .map { collator ->
                        SelectedCollator(
                            collator = collator,
                            delegation = delegationAmountByCollator.getValue(collator.accountIdHex)
                        )
                    }
                    .sortedWith(sorting.ascendingComparator().reversed())
            }

            is DelegatorState.None -> emptyList()
        }
    }

    private fun SelectedCollatorSorting.ascendingComparator() = when (this) {
        SelectedCollatorSorting.DELEGATION -> compareBy<SelectedCollator> { it.delegation }
        SelectedCollatorSorting.APR -> compareBy { it.collator.apr }
    }
}
