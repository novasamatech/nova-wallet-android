package io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.valiadtion.AdvancedEncryptionValidationSystem
import io.novafoundation.nova.feature_account_impl.domain.common.AdvancedEncryptionSelectionStoreProvider
import io.novafoundation.nova.feature_account_api.presenatation.AccountRouter
import io.novafoundation.nova.feature_account_api.presenatation.account.advancedEncryption.AdvancedEncryptionModePayload
import io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.AdvancedEncryptionViewModel

@Module(includes = [ViewModelModule::class, ValidationsModule::class])
class AdvancedEncryptionModule {

    @Provides
    @IntoMap
    @ViewModelKey(AdvancedEncryptionViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        payload: AdvancedEncryptionModePayload,
        interactor: AdvancedEncryptionInteractor,
        resourceManager: ResourceManager,
        validationSystem: AdvancedEncryptionValidationSystem,
        validationExecutor: ValidationExecutor,
        advancedEncryptionSelectionStoreProvider: AdvancedEncryptionSelectionStoreProvider
    ): ViewModel {
        return AdvancedEncryptionViewModel(
            router,
            payload,
            interactor,
            resourceManager,
            validationSystem,
            validationExecutor,
            advancedEncryptionSelectionStoreProvider
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): AdvancedEncryptionViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AdvancedEncryptionViewModel::class.java)
    }
}
