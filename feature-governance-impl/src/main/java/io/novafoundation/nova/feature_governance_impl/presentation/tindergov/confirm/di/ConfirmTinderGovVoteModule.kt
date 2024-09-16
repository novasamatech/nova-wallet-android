package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.confirm.di

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
import io.novafoundation.nova.common.utils.multiResult.PartialRetriableMixin
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_api.domain.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.VoteReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.hints.ReferendumVoteHintsMixinFactory
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.confirm.ConfirmTinderGovVoteViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class ConfirmTinderGovVoteModule {

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmTinderGovVoteViewModel::class)
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
        validationSystem: VoteReferendumValidationSystem,
        validationExecutor: ValidationExecutor,
        resourceManager: ResourceManager,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        locksChangeFormatter: LocksChangeFormatter,
        tinderGovInteractor: TinderGovInteractor,
        partialRetriableMixinFactory: PartialRetriableMixin.Factory,
    ): ViewModel {
        return ConfirmTinderGovVoteViewModel(
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
            validationSystem = validationSystem,
            validationExecutor = validationExecutor,
            resourceManager = resourceManager,
            locksChangeFormatter = locksChangeFormatter,
            tinderGovInteractor = tinderGovInteractor,
            partialRetriableMixinFactory = partialRetriableMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ConfirmTinderGovVoteViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmTinderGovVoteViewModel::class.java)
    }
}
