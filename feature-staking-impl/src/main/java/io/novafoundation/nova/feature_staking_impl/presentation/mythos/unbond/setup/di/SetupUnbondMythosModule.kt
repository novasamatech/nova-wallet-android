package io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.setup.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosDelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.UnbondMythosStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.validations.UnbondMythosValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.MythosCollatorFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.validations.MythosStakingValidationFailureFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.setup.SetupUnbondMythosViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class SetupUnbondMythosModule {

    @Provides
    @IntoMap
    @ViewModelKey(SetupUnbondMythosViewModel::class)
    fun provideViewModel(
        router: MythosStakingRouter,
        interactor: UnbondMythosStakingInteractor,
        assetUseCase: AssetUseCase,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        validationSystem: UnbondMythosValidationSystem,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        delegatorStateUseCase: MythosDelegatorStateUseCase,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        mythosSharedComputation: MythosSharedComputation,
        mythosCollatorFormatter: MythosCollatorFormatter,
        mythosValidationFailureFormatter: MythosStakingValidationFailureFormatter,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
        stakingSharedState: StakingSharedState
    ): ViewModel {
        return SetupUnbondMythosViewModel(
            router = router,
            interactor = interactor,
            assetUseCase = assetUseCase,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            validationSystem = validationSystem,
            feeLoaderMixin = feeLoaderMixin,
            delegatorStateUseCase = delegatorStateUseCase,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            mythosSharedComputation = mythosSharedComputation,
            mythosCollatorFormatter = mythosCollatorFormatter,
            mythosValidationFailureFormatter = mythosValidationFailureFormatter,
            amountChooserMixinFactory = amountChooserMixinFactory,
            stakingSharedState = stakingSharedState
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SetupUnbondMythosViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SetupUnbondMythosViewModel::class.java)
    }
}
