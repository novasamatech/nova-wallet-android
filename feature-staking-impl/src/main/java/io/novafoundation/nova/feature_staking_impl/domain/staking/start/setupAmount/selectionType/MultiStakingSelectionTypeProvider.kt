package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.selectionType

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.MultiStakingOptionIds
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.constructStakingOption
import io.novafoundation.nova.feature_staking_impl.data.constructStakingOptions
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.SelectionTypeSource
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStore
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingPropertiesFactory
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map

interface MultiStakingSelectionTypeProvider {

    fun multiStakingSelectionTypeFlow(): Flow<MultiStakingSelectionType>
}

class MultiStakingSelectionTypeProviderFactory(
    private val selectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val singleStakingPropertiesFactory: SingleStakingPropertiesFactory,
    private val chainRegistry: ChainRegistry,
    private val locksRepository: BalanceLocksRepository,
) {

    fun create(
        scope: CoroutineScope,
        candidateOptionsIds: MultiStakingOptionIds
    ): MultiStakingSelectionTypeProvider {
        return RealMultiStakingSelectionTypeProvider(
            selectionStoreProvider = selectionStoreProvider,
            candidateOptionIds = candidateOptionsIds,
            scope = scope,
            singleStakingPropertiesFactory = singleStakingPropertiesFactory,
            locksRepository = locksRepository,
            chainRegistry = chainRegistry
        )
    }
}

private class RealMultiStakingSelectionTypeProvider(
    private val selectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val candidateOptionIds: MultiStakingOptionIds,
    private val scope: CoroutineScope,
    private val singleStakingPropertiesFactory: SingleStakingPropertiesFactory,
    private val locksRepository: BalanceLocksRepository,
    private val chainRegistry: ChainRegistry,
) : MultiStakingSelectionTypeProvider {

    override fun multiStakingSelectionTypeFlow(): Flow<MultiStakingSelectionType> {
        return flowOfAll {
            val selectionStore = selectionStoreProvider.getSelectionStore(scope)

            selectionStore.currentSelectionFlow
                .distinctUntilChangedBy { it.diffId() }
                .map { createSelectionType(it, selectionStore) }
        }
    }

    private suspend fun createSelectionType(
        currentSelection: RecommendableMultiStakingSelection?,
        selectionStore: StartMultiStakingSelectionStore,
    ): MultiStakingSelectionType {
        return when (currentSelection?.source) {
            null, SelectionTypeSource.Automatic -> createAutomaticSelection(selectionStore)

            is SelectionTypeSource.Manual -> createManualSelection(currentSelection.selection.stakingOption.stakingType)
        }
    }

    private suspend fun createAutomaticSelection(
        selectionStore: StartMultiStakingSelectionStore,
    ): MultiStakingSelectionType {
        val candidateOptions = chainRegistry.constructStakingOptions(candidateOptionIds)
            .sortedBy { it.stakingType.multiStakingPriority() }

        val candidates = candidateOptions.map { option ->
            singleStakingPropertiesFactory.createProperties(scope, option)
        }

        return AutomaticMultiStakingSelectionType(candidates, selectionStore, locksRepository)
    }

    private suspend fun createManualSelection(
        selectedStakingType: Chain.Asset.StakingType,
    ): MultiStakingSelectionType {
        val optionId = StakingOptionId(candidateOptionIds.chainId, candidateOptionIds.chainAssetId, selectedStakingType)
        val option = chainRegistry.constructStakingOption(optionId)

        val stakingProperties = singleStakingPropertiesFactory.createProperties(scope, option)

        return ManualMultiStakingSelectionType(stakingProperties)
    }

    private fun Chain.Asset.StakingType.multiStakingPriority(): Int {
        return when (group()) {
            StakingTypeGroup.RELAYCHAIN -> 0
            StakingTypeGroup.NOMINATION_POOL -> 1
            else -> 2
        }
    }

    /**
     * Ensures [MultiStakingSelectionType] wont be recreated when not needed
     */
    private fun RecommendableMultiStakingSelection?.diffId(): Int {
        return when {
            // when automatic (or null) is selected we consider them same
            this == null || source is SelectionTypeSource.Automatic -> -1

            // when manual is selected we only care about staking type when constructing `MultiStakingSelectionType`
            source is SelectionTypeSource.Manual -> selection.stakingOption.stakingType.ordinal

            // in case we forgot to include something there - prevent false positive equivalence check
            else -> hashCode()
        }
    }
}
