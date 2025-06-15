package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.removeVotes.RemoveTrackVotesInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.removeVotes.RealRemoveTrackVotesInteractor
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.removeVotes.validations.RemoteVotesValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.removeVotes.validations.removeVotesValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.track.TracksUseCase
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes.RemoveVotesPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes.RemoveVotesViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class RemoveVotesModule {

    @Provides
    @ScreenScope
    fun provideValidationSystem() = ValidationSystem.removeVotesValidationSystem()

    @Provides
    @ScreenScope
    fun provideInteractor(
        extrinsicService: ExtrinsicService,
        governanceSharedState: GovernanceSharedState,
        governanceSourceRegistry: GovernanceSourceRegistry,
        accountRepository: AccountRepository,
    ): RemoveTrackVotesInteractor {
        return RealRemoveTrackVotesInteractor(extrinsicService, governanceSharedState, governanceSourceRegistry, accountRepository)
    }

    @Provides
    @IntoMap
    @ViewModelKey(RemoveVotesViewModel::class)
    fun provideViewModel(
        interactor: RemoveTrackVotesInteractor,
        trackFormatter: TrackFormatter,
        payload: RemoveVotesPayload,
        governanceSharedState: GovernanceSharedState,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        assetUseCase: AssetUseCase,
        walletUiUseCase: WalletUiUseCase,
        accountUseCase: SelectedAccountUseCase,
        router: GovernanceRouter,
        externalActions: ExternalActions.Presentation,
        validationExecutor: ValidationExecutor,
        validationSystem: RemoteVotesValidationSystem,
        resourceManager: ResourceManager,
        tracksUseCase: TracksUseCase,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper
    ): ViewModel {
        return RemoveVotesViewModel(
            interactor = interactor,
            trackFormatter = trackFormatter,
            payload = payload,
            governanceSharedState = governanceSharedState,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            assetUseCase = assetUseCase,
            walletUiUseCase = walletUiUseCase,
            accountUseCase = accountUseCase,
            router = router,
            externalActions = externalActions,
            validationExecutor = validationExecutor,
            validationSystem = validationSystem,
            resourceManager = resourceManager,
            tracksUseCase = tracksUseCase,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): RemoveVotesViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(RemoveVotesViewModel::class.java)
    }
}
