package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.confirm.di

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
import io.novafoundation.nova.common.utils.multiResult.PartialRetriableMixin
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.label.DelegateLabelUseCase
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseAmount.NewDelegationChooseAmountInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount.validation.ChooseDelegationAmountValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.track.TracksUseCase
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VotersFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.confirm.NewDelegationConfirmPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.confirm.NewDelegationConfirmViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter

@Module(includes = [ViewModelModule::class])
class NewDelegationConfirmModule {

    @Provides
    @IntoMap
    @ViewModelKey(NewDelegationConfirmViewModel::class)
    fun provideViewModel(
        router: GovernanceRouter,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        externalActions: ExternalActions.Presentation,
        governanceSharedState: GovernanceSharedState,
        walletUiUseCase: WalletUiUseCase,
        selectedAccountUseCase: SelectedAccountUseCase,
        interactor: NewDelegationChooseAmountInteractor,
        trackFormatter: TrackFormatter,
        assetUseCase: AssetUseCase,
        payload: NewDelegationConfirmPayload,
        validationSystem: ChooseDelegationAmountValidationSystem,
        validationExecutor: ValidationExecutor,
        resourceManager: ResourceManager,
        locksChangeFormatter: LocksChangeFormatter,
        resourcesHintsMixinFactory: ResourcesHintsMixinFactory,
        votersFormatter: VotersFormatter,
        tracksUseCase: TracksUseCase,
        delegateFormatters: DelegateMappers,
        delegateLabelUseCase: DelegateLabelUseCase,
        partialRetriableMixinFactory: PartialRetriableMixin.Factory,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
        amountFormatter: AmountFormatter
    ): ViewModel {
        return NewDelegationConfirmViewModel(
            router = router,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            externalActions = externalActions,
            governanceSharedState = governanceSharedState,
            walletUiUseCase = walletUiUseCase,
            selectedAccountUseCase = selectedAccountUseCase,
            interactor = interactor,
            trackFormatter = trackFormatter,
            assetUseCase = assetUseCase,
            payload = payload,
            validationSystem = validationSystem,
            validationExecutor = validationExecutor,
            resourceManager = resourceManager,
            locksChangeFormatter = locksChangeFormatter,
            resourcesHintsMixinFactory = resourcesHintsMixinFactory,
            votersFormatter = votersFormatter,
            tracksUseCase = tracksUseCase,
            delegateFormatters = delegateFormatters,
            delegateLabelUseCase = delegateLabelUseCase,
            partialRetriableMixinFactory = partialRetriableMixinFactory,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper,
            amountFormatter = amountFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): NewDelegationConfirmViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(NewDelegationConfirmViewModel::class.java)
    }
}
