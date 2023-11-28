package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.pool

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetailsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetailsProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetailsProviderFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.direct.StakingTypeDetailsInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingPropertiesFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingRecommendation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct.EditingStakingTypeFailure
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct.EditingStakingTypePayload
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct.EditingStakingTypeValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct.editingStakingType
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.model.ValidatedStakingTypeDetails
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealStakingTypeDetailsProviderFactory(
    private val poolStakingTypeDetailsInteractorFactory: StakingTypeDetailsInteractorFactory,
    private val singleStakingPropertiesFactory: SingleStakingPropertiesFactory,
    private val currentSelectionStoreProvider: StartMultiStakingSelectionStoreProvider
) : StakingTypeDetailsProviderFactory {

    override suspend fun create(
        stakingOption: StakingOption,
        coroutineScope: CoroutineScope,
        availableStakingTypes: List<Chain.Asset.StakingType>
    ): StakingTypeDetailsProvider {
        val singleStakingProperties = singleStakingPropertiesFactory.createProperties(coroutineScope, stakingOption)
        val validationSystem = ValidationSystem.editingStakingType(availableStakingTypes)
        return RealStakingTypeDetailsProvider(
            validationSystem,
            poolStakingTypeDetailsInteractorFactory.create(stakingOption, coroutineScope),
            stakingOption.stakingType,
            singleStakingProperties,
            currentSelectionStoreProvider,
            coroutineScope
        )
    }
}

class RealStakingTypeDetailsProvider(
    private val validationSystem: EditingStakingTypeValidationSystem,
    stakingTypeDetailsInteractor: StakingTypeDetailsInteractor,
    override val stakingType: Chain.Asset.StakingType,
    private val singleStakingProperties: SingleStakingProperties,
    private val currentSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val coroutineScope: CoroutineScope
) : StakingTypeDetailsProvider {

    override val recommendationProvider: SingleStakingRecommendation = singleStakingProperties.recommendation

    override val stakingTypeDetails: Flow<ValidatedStakingTypeDetails> = stakingTypeDetailsInteractor.observeData()
        .map {
            ValidatedStakingTypeDetails(
                isAvailable = validate() is ValidationStatus.Valid,
                stakingTypeDetails = it
            )
        }

    private suspend fun validate(): ValidationStatus<EditingStakingTypeFailure>? {
        val payload = getValidationPayload() ?: return null
        return validationSystem.validate(payload).getOrNull()
    }

    override fun getValidationSystem(): EditingStakingTypeValidationSystem {
        return validationSystem
    }

    override suspend fun getValidationPayload(): EditingStakingTypePayload? {
        val selectionStore = currentSelectionStoreProvider.getSelectionStore(coroutineScope)
        val selectedStake = selectionStore.getCurrentSelection()?.selection?.stake ?: return null
        return EditingStakingTypePayload(selectedStake, stakingType, singleStakingProperties.minStake())
    }
}
