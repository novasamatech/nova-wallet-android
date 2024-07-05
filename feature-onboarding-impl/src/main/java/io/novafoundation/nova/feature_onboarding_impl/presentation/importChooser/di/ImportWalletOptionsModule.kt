package io.novafoundation.nova.feature_onboarding_impl.presentation.importChooser.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.progress.ProgressDialogMixin
import io.novafoundation.nova.common.utils.progress.ProgressDialogMixinFactory
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.CloudBackupChangingWarningMixinFactory
import io.novafoundation.nova.feature_onboarding_api.domain.OnboardingInteractor
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter
import io.novafoundation.nova.feature_onboarding_impl.presentation.importChooser.ImportWalletOptionsViewModel

@Module(includes = [ViewModelModule::class])
class ImportWalletOptionsModule {

    @Provides
    @IntoMap
    @ViewModelKey(ImportWalletOptionsViewModel::class)
    fun provideViewModel(
        resourceManager: ResourceManager,
        router: OnboardingRouter,
        actionAwaitableMixin: ActionAwaitableMixin.Factory,
        progressDialogMixinFactory: ProgressDialogMixinFactory,
        onboardingInteractor: OnboardingInteractor,
        customDialogProvider: CustomDialogDisplayer.Presentation,
        cloudBackupChangingWarningMixinFactory: CloudBackupChangingWarningMixinFactory
    ): ViewModel {
        return ImportWalletOptionsViewModel(
            resourceManager,
            router,
            actionAwaitableMixin,
            onboardingInteractor,
            progressDialogMixinFactory,
            customDialogProvider,
            cloudBackupChangingWarningMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ImportWalletOptionsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ImportWalletOptionsViewModel::class.java)
    }
}
