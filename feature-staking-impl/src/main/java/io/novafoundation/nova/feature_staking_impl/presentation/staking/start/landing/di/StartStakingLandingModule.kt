package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingLandingInfoUpdateSystemFactory
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingUpdaters
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.StakingStartedDetectionService
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.StakingTypeDetailsCompoundInteractorFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.StartStakingLandingViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StartStakingLandingPayload
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class StartStakingLandingModule {

    @Provides
    fun provideStartStakingLandingUpdateSystemFactory(
        stakingUpdaters: StakingUpdaters,
        chainRegistry: ChainRegistry,
        storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory
    ): StakingLandingInfoUpdateSystemFactory {
        return StakingLandingInfoUpdateSystemFactory(
            stakingUpdaters,
            chainRegistry,
            storageSharedRequestsBuilderFactory
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(StartStakingLandingViewModel::class)
    fun provideViewModel(
        router: StartMultiStakingRouter,
        resourceManager: ResourceManager,
        updateSystemFactory: StakingLandingInfoUpdateSystemFactory,
        stakingTypeDetailsCompoundInteractorFactory: StakingTypeDetailsCompoundInteractorFactory,
        appLinksProvider: AppLinksProvider,
        startStakingLandingPayload: StartStakingLandingPayload,
        validationExecutor: ValidationExecutor,
        selectedMetaAccountUseCase: SelectedAccountUseCase,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        stakingStartedDetectionService: StakingStartedDetectionService,
        contextManager: ContextManager
    ): ViewModel {
        return StartStakingLandingViewModel(
            router = router,
            resourceManager = resourceManager,
            updateSystemFactory = updateSystemFactory,
            stakingTypeDetailsCompoundInteractorFactory = stakingTypeDetailsCompoundInteractorFactory,
            appLinksProvider = appLinksProvider,
            startStakingLandingPayload = startStakingLandingPayload,
            validationExecutor = validationExecutor,
            selectedMetaAccountUseCase = selectedMetaAccountUseCase,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            stakingStartedDetectionService = stakingStartedDetectionService,
            contextManager = contextManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): StartStakingLandingViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(StartStakingLandingViewModel::class.java)
    }
}
