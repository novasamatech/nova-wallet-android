package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.di.staking.startMultiStaking.MultiStakingSelectionStoreProviderKey
import io.novafoundation.nova.feature_staking_impl.di.staking.startMultiStaking.StakingTypeEditingStoreProviderKey
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.CompoundStakingTypeDetailsProvidersFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.SetupStakingTypeSelectionMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.MultiStakingTargetSelectionFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.EditableStakingTypeItemFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.SetupStakingTypeFlowExecutorFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.SetupStakingTypePayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.SetupStakingTypeViewModel
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SetupStakingTypeModule {

    @Provides
    fun provideSetupStakingTypeFlowExecutorFactory(
        stakingRouter: StakingRouter,
        setupStakingSharedState: SetupStakingSharedState,
        @StakingTypeEditingStoreProviderKey editableSelectionStoreProvider: StartMultiStakingSelectionStoreProvider
    ): SetupStakingTypeFlowExecutorFactory {
        return SetupStakingTypeFlowExecutorFactory(
            stakingRouter,
            setupStakingSharedState,
            editableSelectionStoreProvider
        )
    }

    @Provides
    fun provideEditableStakingTypeItemFormatter(
        resourceManager: ResourceManager,
        multiStakingTargetSelectionFormatter: MultiStakingTargetSelectionFormatter
    ): EditableStakingTypeItemFormatter {
        return EditableStakingTypeItemFormatter(
            resourceManager,
            multiStakingTargetSelectionFormatter
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(SetupStakingTypeViewModel::class)
    fun provideViewModel(
        stakingRouter: StakingRouter,
        assetUseCase: ArbitraryAssetUseCase,
        payload: SetupStakingTypePayload,
        @MultiStakingSelectionStoreProviderKey currentSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
        @StakingTypeEditingStoreProviderKey editableSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
        editableStakingTypeItemFormatter: EditableStakingTypeItemFormatter,
        compoundStakingTypeDetailsProvidersFactory: CompoundStakingTypeDetailsProvidersFactory,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        setupStakingTypeFlowExecutorFactory: SetupStakingTypeFlowExecutorFactory,
        setupStakingTypeSelectionMixinFactory: SetupStakingTypeSelectionMixinFactory,
        chainRegistry: ChainRegistry
    ): ViewModel {
        return SetupStakingTypeViewModel(
            stakingRouter,
            assetUseCase,
            payload,
            currentSelectionStoreProvider,
            editableSelectionStoreProvider,
            editableStakingTypeItemFormatter,
            compoundStakingTypeDetailsProvidersFactory,
            resourceManager,
            validationExecutor,
            setupStakingTypeFlowExecutorFactory,
            setupStakingTypeSelectionMixinFactory,
            chainRegistry
        )
    }

    @Provides
    fun viewModelCreator(
        fragment: Fragment,
        viewModelProviderFactory: ViewModelProvider.Factory
    ): SetupStakingTypeViewModel {
        return ViewModelProvider(fragment, viewModelProviderFactory).get(SetupStakingTypeViewModel::class.java)
    }
}
