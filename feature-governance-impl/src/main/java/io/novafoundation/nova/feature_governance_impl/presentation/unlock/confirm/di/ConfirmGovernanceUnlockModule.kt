package io.novafoundation.nova.feature_governance_impl.presentation.unlock.confirm.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.multiResult.PartialRetriableMixin
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.GovernanceUnlockInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.validations.UnlockReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.validations.unlockReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.confirm.ConfirmGovernanceUnlockViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.confirm.hints.ConfirmGovernanceUnlockHintsMixinFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class ConfirmGovernanceUnlockModule {

    @Provides
    @ScreenScope
    fun provideValidationSystem(): UnlockReferendumValidationSystem = ValidationSystem.unlockReferendumValidationSystem()

    @Provides
    @ScreenScope
    fun provieHintsFactory(
        resourceManager: ResourceManager
    ) = ConfirmGovernanceUnlockHintsMixinFactory(resourceManager)

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmGovernanceUnlockViewModel::class)
    fun provideViewModel(
        router: GovernanceRouter,
        externalActions: ExternalActions.Presentation,
        governanceSharedState: GovernanceSharedState,
        validationExecutor: ValidationExecutor,
        interactor: GovernanceUnlockInteractor,
        feeMixinFactory: FeeLoaderMixin.Factory,
        assetUseCase: AssetUseCase,
        walletUiUseCase: WalletUiUseCase,
        selectedAccountUseCase: SelectedAccountUseCase,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager,
        locksChangeFormatter: LocksChangeFormatter,
        validationSystem: UnlockReferendumValidationSystem,
        hintsMixinFactory: ConfirmGovernanceUnlockHintsMixinFactory,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
        partialRetriableMixinFactory: PartialRetriableMixin.Factory,
    ): ViewModel {
        return ConfirmGovernanceUnlockViewModel(
            router = router,
            externalActions = externalActions,
            governanceSharedState = governanceSharedState,
            validationExecutor = validationExecutor,
            interactor = interactor,
            feeMixinFactory = feeMixinFactory,
            assetUseCase = assetUseCase,
            walletUiUseCase = walletUiUseCase,
            selectedAccountUseCase = selectedAccountUseCase,
            addressIconGenerator = addressIconGenerator,
            resourceManager = resourceManager,
            locksChangeFormatter = locksChangeFormatter,
            validationSystem = validationSystem,
            hintsMixinFactory = hintsMixinFactory,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper,
            partialRetriableMixinFactory = partialRetriableMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ConfirmGovernanceUnlockViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmGovernanceUnlockViewModel::class.java)
    }
}
