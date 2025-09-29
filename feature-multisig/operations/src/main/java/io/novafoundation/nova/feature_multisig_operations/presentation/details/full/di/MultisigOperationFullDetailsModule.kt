package io.novafoundation.nova.feature_multisig_operations.presentation.details.full.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.copy.CopyTextLauncher
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountUIUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_multisig_operations.domain.details.MultisigOperationDetailsInteractor
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.common.MultisigOperationPayload
import io.novafoundation.nova.feature_multisig_operations.presentation.details.full.MultisigOperationFullDetailsViewModel
import io.novafoundation.nova.feature_multisig_operations.presentation.details.general.di.MultisigOperationDetailsModule.BindsModule
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryTokenUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter

@Module(includes = [ViewModelModule::class, BindsModule::class])
class MultisigOperationFullDetailsModule {

    @Provides
    @IntoMap
    @ViewModelKey(MultisigOperationFullDetailsViewModel::class)
    fun provideViewModel(
        router: MultisigOperationsRouter,
        resourceManager: ResourceManager,
        interactor: MultisigOperationDetailsInteractor,
        multisigOperationsService: MultisigPendingOperationsService,
        externalActions: ExternalActions.Presentation,
        payload: MultisigOperationPayload,
        descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
        copyTextLauncher: CopyTextLauncher.Presentation,
        accountUIUseCase: AccountUIUseCase,
        arbitraryTokenUseCase: ArbitraryTokenUseCase,
        amountFormatter: AmountFormatter
    ): ViewModel {
        return MultisigOperationFullDetailsViewModel(
            router = router,
            resourceManager = resourceManager,
            interactor = interactor,
            multisigOperationsService = multisigOperationsService,
            externalActions = externalActions,
            payload = payload,
            descriptionBottomSheetLauncher = descriptionBottomSheetLauncher,
            copyTextLauncher = copyTextLauncher,
            accountUIUseCase = accountUIUseCase,
            arbitraryTokenUseCase = arbitraryTokenUseCase,
            amountFormatter = amountFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): MultisigOperationFullDetailsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(MultisigOperationFullDetailsViewModel::class.java)
    }
}
