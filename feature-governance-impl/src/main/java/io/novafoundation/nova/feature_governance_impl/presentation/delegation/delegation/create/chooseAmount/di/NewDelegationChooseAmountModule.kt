package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseAmount.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseAmount.NewDelegationChooseAmountInteractor
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount.validation.ChooseDelegationAmountValidationSystem
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.conviction.ConvictionValuesProvider
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.LocksFormatter
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.create.chooseAmount.NewDelegationChooseAmountPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseAmount.NewDelegationChooseAmountViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class NewDelegationChooseAmountModule {

    @Provides
    @IntoMap
    @ViewModelKey(NewDelegationChooseAmountViewModel::class)
    fun provideViewModel(
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        assetUseCase: AssetUseCase,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
        interactor: NewDelegationChooseAmountInteractor,
        payload: NewDelegationChooseAmountPayload,
        resourceManager: ResourceManager,
        router: GovernanceRouter,
        validationExecutor: ValidationExecutor,
        locksChangeFormatter: LocksChangeFormatter,
        convictionValuesProvider: ConvictionValuesProvider,
        locksFormatter: LocksFormatter,
        validationSystem: ChooseDelegationAmountValidationSystem,
        resourcesHintsMixinFactory: ResourcesHintsMixinFactory,
    ): ViewModel {
        return NewDelegationChooseAmountViewModel(
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            assetUseCase = assetUseCase,
            amountChooserMixinFactory = amountChooserMixinFactory,
            interactor = interactor,
            payload = payload,
            resourceManager = resourceManager,
            router = router,
            validationExecutor = validationExecutor,
            locksChangeFormatter = locksChangeFormatter,
            convictionValuesProvider = convictionValuesProvider,
            locksFormatter = locksFormatter,
            validationSystem = validationSystem,
            resourcesHintsMixinFactory = resourcesHintsMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): NewDelegationChooseAmountViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(NewDelegationChooseAmountViewModel::class.java)
    }
}
