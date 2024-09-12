package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.VoteReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.ConfirmReferendumVoteViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.ConfirmVoteReferendumPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.hints.ReferendumVoteHintsMixinFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class ConfirmReferendumVoteModule {

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmReferendumVoteViewModel::class)
    fun provideViewModel(
        router: GovernanceRouter,
        externalActions: ExternalActions.Presentation,
        governanceSharedState: GovernanceSharedState,
        hintsMixinFactory: ReferendumVoteHintsMixinFactory,
        walletUiUseCase: WalletUiUseCase,
        selectedAccountUseCase: SelectedAccountUseCase,
        addressIconGenerator: AddressIconGenerator,
        interactor: VoteReferendumInteractor,
        assetUseCase: AssetUseCase,
        payload: ConfirmVoteReferendumPayload,
        validationSystem: VoteReferendumValidationSystem,
        validationExecutor: ValidationExecutor,
        resourceManager: ResourceManager,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        referendumFormatter: ReferendumFormatter,
        locksChangeFormatter: LocksChangeFormatter,
    ): ViewModel {
        return ConfirmReferendumVoteViewModel(
            router = router,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            externalActions = externalActions,
            governanceSharedState = governanceSharedState,
            hintsMixinFactory = hintsMixinFactory,
            walletUiUseCase = walletUiUseCase,
            selectedAccountUseCase = selectedAccountUseCase,
            addressIconGenerator = addressIconGenerator,
            interactor = interactor,
            assetUseCase = assetUseCase,
            payload = payload,
            validationSystem = validationSystem,
            validationExecutor = validationExecutor,
            resourceManager = resourceManager,
            referendumFormatter = referendumFormatter,
            locksChangeFormatter = locksChangeFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ConfirmReferendumVoteViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmReferendumVoteViewModel::class.java)
    }
}
