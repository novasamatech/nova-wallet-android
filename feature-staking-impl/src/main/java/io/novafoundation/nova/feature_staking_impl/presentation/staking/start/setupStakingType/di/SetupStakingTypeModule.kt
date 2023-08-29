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
import io.novafoundation.nova.feature_staking_impl.di.staking.startMultiStaking.StakingTypeEditingStoreProviderKey
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.EditingStakingTypeSelectionMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.MultiStakingSelectionFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.SetupStakingTypePayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.SetupStakingTypeViewModel
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SetupStakingTypeModule {

    @Provides
    @IntoMap
    @ViewModelKey(SetupStakingTypeViewModel::class)
    fun provideViewModel(
        stakingRouter: StakingRouter,
        assetUseCase: ArbitraryAssetUseCase,
        resourceManager: ResourceManager,
        payload: SetupStakingTypePayload,
        @StakingTypeEditingStoreProviderKey editableSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
        editingStakingTypeSelectionMixinFactory: EditingStakingTypeSelectionMixinFactory,
        multiStakingSelectionFormatter: MultiStakingSelectionFormatter,
        validationExecutor: ValidationExecutor,
        chainRegistry: ChainRegistry
    ): ViewModel {
        return SetupStakingTypeViewModel(
            stakingRouter,
            assetUseCase,
            resourceManager,
            payload,
            editableSelectionStoreProvider,
            editingStakingTypeSelectionMixinFactory,
            multiStakingSelectionFormatter,
            validationExecutor,
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
