package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rebond.ParachainStakingRebondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rebond.RealParachainStakingRebondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rebond.validations.ParachainStakingRebondValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rebond.validations.parachainStakingRebond
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond.ParachainStakingRebondViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond.model.ParachainStakingRebondPayload
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class ParachainStakingRebondModule {

    @Provides
    @ScreenScope
    fun provideValidationSystem(): ParachainStakingRebondValidationSystem {
        return ValidationSystem.parachainStakingRebond()
    }

    @Provides
    @ScreenScope
    fun provideInteractor(
        extrinsicService: ExtrinsicService,
        delegatorStateRepository: DelegatorStateRepository,
        selectedAssetState: StakingSharedState,
    ): ParachainStakingRebondInteractor = RealParachainStakingRebondInteractor(
        extrinsicService = extrinsicService,
        delegatorStateRepository = delegatorStateRepository,
        selectedAssetState = selectedAssetState
    )

    @Provides
    @IntoMap
    @ViewModelKey(ParachainStakingRebondViewModel::class)
    fun provideViewModel(
        router: ParachainStakingRouter,
        resourceManager: ResourceManager,
        validationSystem: ParachainStakingRebondValidationSystem,
        interactor: ParachainStakingRebondInteractor,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        externalActions: ExternalActions.Presentation,
        selectedAssetState: StakingSharedState,
        validationExecutor: ValidationExecutor,
        delegatorStateUseCase: DelegatorStateUseCase,
        payload: ParachainStakingRebondPayload,
        collatorsUseCase: CollatorsUseCase,
        selectedAccountUseCase: SelectedAccountUseCase,
        assetUseCase: AssetUseCase,
        walletUiUseCase: WalletUiUseCase,
        resourcesHintsMixinFactory: ResourcesHintsMixinFactory,
    ): ViewModel {
        return ParachainStakingRebondViewModel(
            router = router,
            resourceManager = resourceManager,
            interactor = interactor,
            feeLoaderMixin = feeLoaderMixin,
            externalActions = externalActions,
            selectedAssetState = selectedAssetState,
            validationExecutor = validationExecutor,
            delegatorStateUseCase = delegatorStateUseCase,
            payload = payload,
            collatorsUseCase = collatorsUseCase,
            selectedAccountUseCase = selectedAccountUseCase,
            assetUseCase = assetUseCase,
            walletUiUseCase = walletUiUseCase,
            validationSystem = validationSystem,
            resourcesHintsMixinFactory = resourcesHintsMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ParachainStakingRebondViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ParachainStakingRebondViewModel::class.java)
    }
}
