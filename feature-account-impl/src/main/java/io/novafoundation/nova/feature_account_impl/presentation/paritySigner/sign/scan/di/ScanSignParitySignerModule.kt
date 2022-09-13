package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.utils.MutableSharedState
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.ParitySignerSignCommunicator
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.scan.RealScanSignParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.scan.ScanSignParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.common.QrCodeExpiredPresentableFactory
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.ScanSignParitySignerViewModel
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.model.ScanSignParitySignerPayload
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic

@Module(includes = [ViewModelModule::class])
class ScanSignParitySignerModule {

    @Provides
    fun provideInteractor(): ScanSignParitySignerInteractor = RealScanSignParitySignerInteractor()

    @Provides
    fun providePermissionAsker(
        permissionsAskerFactory: PermissionsAskerFactory,
        fragment: Fragment,
        router: AccountRouter
    ) = permissionsAskerFactory.create(fragment, router)

    @Provides
    @IntoMap
    @ViewModelKey(ScanSignParitySignerViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        permissionsAsker: PermissionsAsker.Presentation,
        interactor: ScanSignParitySignerInteractor,
        signSharedState: MutableSharedState<SignerPayloadExtrinsic>,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        communicator: ParitySignerSignCommunicator,
        payload: ScanSignParitySignerPayload,
        qrCodeExpiredPresentableFactory: QrCodeExpiredPresentableFactory,
    ): ViewModel {
        return ScanSignParitySignerViewModel(
            router = router,
            permissionsAsker = permissionsAsker,
            interactor = interactor,
            signSharedState = signSharedState,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            responder = communicator,
            payload = payload,
            qrCodeExpiredPresentableFactory = qrCodeExpiredPresentableFactory
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): ScanSignParitySignerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ScanSignParitySignerViewModel::class.java)
    }
}
