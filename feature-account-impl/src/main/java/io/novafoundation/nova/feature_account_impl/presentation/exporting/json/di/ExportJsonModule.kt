package io.novafoundation.nova.feature_account_impl.presentation.exporting.json.di

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
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.ExportJsonViewModel

@Module(includes = [ViewModelModule::class, ValidationsModule::class])
class ExportJsonModule {

    @Provides
    @IntoMap
    @ViewModelKey(ExportJsonViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        accountInteractor: ExportJsonInteractor,
        validationExecutor: ValidationExecutor,
        validationSystem: ExportJsonPasswordValidationSystem,
        resourceManager: ResourceManager,
        payload: ExportPayload
    ): ViewModel {
        return ExportJsonViewModel(
            router,
            accountInteractor,
            resourceManager,
            validationExecutor,
            validationSystem,
            payload
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): ExportJsonViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ExportJsonViewModel::class.java)
    }
}
