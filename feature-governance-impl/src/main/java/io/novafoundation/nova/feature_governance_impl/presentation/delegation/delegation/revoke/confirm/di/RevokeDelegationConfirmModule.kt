package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.confirm.di

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
import io.novafoundation.nova.common.utils.multiResult.PartialRetriableMixin
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.label.DelegateLabelUseCase
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.revoke.RealRevokeDelegationsInteractor
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.revoke.RevokeDelegationsInteractor
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.revoke.validations.RevokeDelegationValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.revoke.validations.revokeDelegationValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.track.TracksUseCase
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.confirm.RevokeDelegationConfirmPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.confirm.RevokeDelegationConfirmViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.repository.ChainStateRepository

@Module(includes = [ViewModelModule::class])
class RevokeDelegationConfirmModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        extrinsicService: ExtrinsicService,
        governanceSharedState: GovernanceSharedState,
        governanceSourceRegistry: GovernanceSourceRegistry,
        chainStateRepository: ChainStateRepository,
        accountRepository: AccountRepository,
        tracksUseCase: TracksUseCase,
    ): RevokeDelegationsInteractor = RealRevokeDelegationsInteractor(
        extrinsicService = extrinsicService,
        governanceSharedState = governanceSharedState,
        governanceSourceRegistry = governanceSourceRegistry,
        chainStateRepository = chainStateRepository,
        accountRepository = accountRepository,
        tracksUseCase = tracksUseCase
    )

    @Provides
    @ScreenScope
    fun provideValidationSystem() = ValidationSystem.revokeDelegationValidationSystem()

    @Provides
    @IntoMap
    @ViewModelKey(RevokeDelegationConfirmViewModel::class)
    fun provideViewModel(
        router: GovernanceRouter,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        externalActions: ExternalActions.Presentation,
        governanceSharedState: GovernanceSharedState,
        walletUiUseCase: WalletUiUseCase,
        selectedAccountUseCase: SelectedAccountUseCase,
        interactor: RevokeDelegationsInteractor,
        trackFormatter: TrackFormatter,
        assetUseCase: AssetUseCase,
        payload: RevokeDelegationConfirmPayload,
        validationSystem: RevokeDelegationValidationSystem,
        validationExecutor: ValidationExecutor,
        resourceManager: ResourceManager,
        resourcesHintsMixinFactory: ResourcesHintsMixinFactory,
        delegateFormatters: DelegateMappers,
        delegateLabelUseCase: DelegateLabelUseCase,
        partialRetriableMixinFactory: PartialRetriableMixin.Factory,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper
    ): ViewModel {
        return RevokeDelegationConfirmViewModel(
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
            resourcesHintsMixinFactory = resourcesHintsMixinFactory,
            partialRetriableMixinFactory = partialRetriableMixinFactory,
            delegateFormatters = delegateFormatters,
            delegateLabelUseCase = delegateLabelUseCase,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): RevokeDelegationConfirmViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(RevokeDelegationConfirmViewModel::class.java)
    }
}
