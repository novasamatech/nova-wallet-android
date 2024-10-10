package io.novafoundation.nova.feature_account_impl.presentation.pincode.di

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.io.MainThreadExecutor
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationExecutor
import io.novafoundation.nova.common.sequrity.biometry.BiometricPromptFactory
import io.novafoundation.nova.common.sequrity.biometry.BiometricService
import io.novafoundation.nova.common.sequrity.biometry.BiometricServiceFactory
import io.novafoundation.nova.common.utils.sequrity.BackgroundAccessObserver
import io.novafoundation.nova.common.vibration.DeviceVibrator
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PinCodeAction
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PinCodeViewModel

@Module(
    includes = [
        ViewModelModule::class
    ]
)
class PinCodeModule {

    @Provides
    @IntoMap
    @ViewModelKey(PinCodeViewModel::class)
    fun provideViewModel(
        interactor: AccountInteractor,
        router: AccountRouter,
        deviceVibrator: DeviceVibrator,
        resourceManager: ResourceManager,
        backgroundAccessObserver: BackgroundAccessObserver,
        pinCodeAction: PinCodeAction,
        biometricService: BiometricService,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        twoFactorVerificationExecutor: TwoFactorVerificationExecutor
    ): ViewModel {
        return PinCodeViewModel(
            interactor,
            router,
            deviceVibrator,
            resourceManager,
            backgroundAccessObserver,
            twoFactorVerificationExecutor,
            actionAwaitableMixinFactory,
            biometricService,
            pinCodeAction
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): PinCodeViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(PinCodeViewModel::class.java)
    }

    @Provides
    fun provideBiometricService(
        fragment: Fragment,
        context: Context,
        resourceManager: ResourceManager,
        realBiometricServiceFactory: BiometricServiceFactory
    ): BiometricService {
        val biometricManager = BiometricManager.from(context)
        val biometricPromptFactory = BiometricPromptFactory(fragment, MainThreadExecutor())
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(resourceManager.getString(R.string.biometric_auth_title))
            .setNegativeButtonText(resourceManager.getString(R.string.common_cancel))
            .build()

        return realBiometricServiceFactory.create(biometricManager, biometricPromptFactory, promptInfo)
    }
}
