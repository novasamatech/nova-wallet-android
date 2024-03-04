package io.novafoundation.nova.feature_account_impl.presentation.exporting.json.password.di

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
import io.novafoundation.nova.feature_account_impl.domain.account.export.json.ExportJsonInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.export.json.validations.ExportJsonPasswordValidationSystem
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.password.ExportJsonPasswordViewModel

@Module(includes = [ViewModelModule::class, ValidationsModule::class])
class ExportJsonPasswordModule {

    @Provides
    @IntoMap
    @ViewModelKey(ExportJsonPasswordViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        accountInteractor: ExportJsonInteractor,
        validationExecutor: ValidationExecutor,
        validationSystem: ExportJsonPasswordValidationSystem,
        resourceManager: ResourceManager,
        payload: ExportPayload
    ): ViewModel {
        return ExportJsonPasswordViewModel(
            router,
            accountInteractor,
            resourceManager,
            validationExecutor,
            validationSystem,
            payload
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): ExportJsonPasswordViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ExportJsonPasswordViewModel::class.java)
    }
}
