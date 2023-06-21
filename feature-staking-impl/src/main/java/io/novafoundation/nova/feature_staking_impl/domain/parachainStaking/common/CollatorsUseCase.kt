package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.systemForcedMinStake
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.SelectedCollator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.collators.collatorAddressModel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAsset
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
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
    private val singleAssetSharedState: AnySelectedAssetOptionSharedState,
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
    private val collatorProvider: CollatorProvider,
    private val addressIconGenerator: AddressIconGenerator,
) : CollatorsUseCase {

    override suspend fun maxRewardedDelegatorsPerCollator(): Int {
        val chainId = singleAssetSharedState.chainId()

        return parachainStakingConstantsRepository.maxRewardedDelegatorsPerCollator(chainId).toInt()
    }

    override suspend fun defaultMinimumStake(): BigInteger {
        return parachainStakingConstantsRepository.systemForcedMinStake(singleAssetSharedState.chainId())
    }

    override suspend fun collatorAddressModel(collator: Collator): AddressModel {
        return addressIconGenerator.collatorAddressModel(
            collator = collator,
            chain = singleAssetSharedState.chain()
        )
    }

    override suspend fun getCollator(collatorId: AccountId): Collator = withContext(Dispatchers.IO) {
        collatorProvider.getCollator(singleAssetSharedState.chainAsset(), collatorId)
    }

    override suspend fun getSelectedCollators(
        delegatorState: DelegatorState,
        sorting: SelectedCollatorSorting
    ): List<SelectedCollator> {
        return when (delegatorState) {
            is DelegatorState.Delegator -> {
                val delegationAmountByCollator = delegatorState.delegations.associateBy(
                    keySelector = { it.owner.toHexString() },
                    valueTransform = { it.balance }
                )
                val stakedCollatorsIds = delegationAmountByCollator.keys

                val collatorSource = CollatorProvider.CollatorSource.Custom(stakedCollatorsIds)

                collatorProvider.getCollators(delegatorState.chainAsset, collatorSource)
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
