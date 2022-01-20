package io.novafoundation.nova.feature_wallet_impl.presentation.send.recipient.di

import android.content.ContentResolver
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_impl.domain.send.SendInteractor
import io.novafoundation.nova.feature_wallet_impl.presentation.AssetPayload
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.send.phishing.warning.api.PhishingWarningMixin
import io.novafoundation.nova.feature_wallet_impl.presentation.send.phishing.warning.impl.PhishingWarningProvider
import io.novafoundation.nova.feature_wallet_impl.presentation.send.recipient.ChooseRecipientViewModel
import io.novafoundation.nova.feature_wallet_impl.presentation.send.recipient.QrBitmapDecoder
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ChooseRecipientModule {

    @Provides
    fun providePhishingAddressMixin(interactor: SendInteractor): PhishingWarningMixin {
        return PhishingWarningProvider(interactor)
    }

    @Provides
    @IntoMap
    @ViewModelKey(ChooseRecipientViewModel::class)
    fun provideViewModel(
        sendInteractor: SendInteractor,
        router: WalletRouter,
        resourceManager: ResourceManager,
        addressIconGenerator: AddressIconGenerator,
        qrBitmapDecoder: QrBitmapDecoder,
        assetPayload: AssetPayload,
        chainRegistry: ChainRegistry,
        phishingWarning: PhishingWarningMixin
    ): ViewModel {
        return ChooseRecipientViewModel(
            sendInteractor,
            router,
            resourceManager,
            addressIconGenerator,
            qrBitmapDecoder,
            assetPayload,
            chainRegistry,
            phishingWarning
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ChooseRecipientViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ChooseRecipientViewModel::class.java)
    }

    @Provides
    fun provideQrCodeDecoder(contentResolver: ContentResolver): QrBitmapDecoder {
        return QrBitmapDecoder(contentResolver)
    }
}
