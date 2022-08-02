package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.utils.QrCodeGenerator
import io.novafoundation.nova.common.utils.SharedState
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.show.RealShowSignParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.show.ShowSignParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show.ShowSignParitySignerViewModel
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic

@Module(includes = [ViewModelModule::class])
class ShowSignParitySignerModule {

    @Provides
    @ScreenScope
    fun provideInteractor(): ShowSignParitySignerInteractor = RealShowSignParitySignerInteractor()

    @Provides
    @IntoMap
    @ViewModelKey(ShowSignParitySignerViewModel::class)
    fun provideViewModel(
        interactor: ShowSignParitySignerInteractor,
        signSharedState: SharedState<SignerPayloadExtrinsic>,
        qrCodeGenerator: QrCodeGenerator
    ): ViewModel {
        return ShowSignParitySignerViewModel(interactor, signSharedState, qrCodeGenerator)
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): ShowSignParitySignerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ShowSignParitySignerViewModel::class.java)
    }
}
