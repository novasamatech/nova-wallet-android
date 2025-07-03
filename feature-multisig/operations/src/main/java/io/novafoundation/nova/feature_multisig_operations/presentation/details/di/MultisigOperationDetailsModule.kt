package io.novafoundation.nova.feature_multisig_operations.presentation.details.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_multisig_operations.domain.details.MultisigOperationDetailsInteractor
import io.novafoundation.nova.feature_multisig_operations.domain.details.RealMultisigOperationDetailsInteractor
import io.novafoundation.nova.feature_multisig_operations.domain.details.validations.ApproveMultisigOperationValidationSystem
import io.novafoundation.nova.feature_multisig_operations.domain.details.validations.OperationIsStillPendingValidation
import io.novafoundation.nova.feature_multisig_operations.domain.details.validations.approveMultisigOperation
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.details.MultisigOperationDetailsPayload
import io.novafoundation.nova.feature_multisig_operations.presentation.details.MultisigOperationDetailsViewModel
import io.novafoundation.nova.feature_multisig_operations.presentation.details.SignatoryListFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.details.di.MultisigOperationDetailsModule.BindsModule
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2

@Module(includes = [ViewModelModule::class, BindsModule::class])
class MultisigOperationDetailsModule {

    @Module
    interface BindsModule {

        @Binds
        fun bindInteractor(real: RealMultisigOperationDetailsInteractor): MultisigOperationDetailsInteractor
    }

    @Provides
    @ScreenScope
    fun provideValidationSystem(
        edValidationFactory: EnoughTotalToStayAboveEDValidationFactory,
        operationIsStillPendingValidation: OperationIsStillPendingValidation
    ): ApproveMultisigOperationValidationSystem {
        return ValidationSystem.approveMultisigOperation(edValidationFactory, operationIsStillPendingValidation)
    }

    @Provides
    @IntoMap
    @ViewModelKey(MultisigOperationDetailsViewModel::class)
    fun provideViewModel(
        router: MultisigOperationsRouter,
        resourceManager: ResourceManager,
        interactor: MultisigOperationDetailsInteractor,
        multisigOperationsService: MultisigPendingOperationsService,
        feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory,
        externalActions: ExternalActions.Presentation,
        validationExecutor: ValidationExecutor,
        payload: MultisigOperationDetailsPayload,
        selectedAccountUseCase: SelectedAccountUseCase,
        validationSystem: ApproveMultisigOperationValidationSystem,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
        signatoryListFormatter: SignatoryListFormatter,
        walletUiUseCase: WalletUiUseCase,
        multisigCallFormatter: MultisigCallFormatter,
        actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory,
        accountInteractor: AccountInteractor,
    ): ViewModel {
        return MultisigOperationDetailsViewModel(
            router = router,
            resourceManager = resourceManager,
            interactor = interactor,
            multisigOperationsService = multisigOperationsService,
            feeLoaderMixinV2Factory = feeLoaderMixinV2Factory,
            externalActions = externalActions,
            validationExecutor = validationExecutor,
            payload = payload,
            selectedAccountUseCase = selectedAccountUseCase,
            walletUiUseCase = walletUiUseCase,
            validationSystem = validationSystem,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper,
            signatoryListFormatter = signatoryListFormatter,
            multisigCallFormatter = multisigCallFormatter,
            actionBottomSheetLauncherFactory = actionBottomSheetLauncherFactory,
            accountInteractor = accountInteractor
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): MultisigOperationDetailsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(MultisigOperationDetailsViewModel::class.java)
    }
}
