package io.novafoundation.nova.feature_ledger_impl.presentation.account.sign.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.MutableSharedState
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.chainId
import io.novafoundation.nova.common.utils.getOrThrow
import io.novafoundation.nova.common.utils.location.LocationManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.sign.LedgerSignCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.domain.account.sign.RealSignLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.sign.SignLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.sign.SignLedgerViewModel
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicValidityUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic

@Module(includes = [ViewModelModule::class])
class SignLedgerModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
    ): SignLedgerInteractor = RealSignLedgerInteractor(accountRepository, chainRegistry)

    @Provides
    @ScreenScope
    fun providePermissionAsker(
        permissionsAskerFactory: PermissionsAskerFactory,
        fragment: Fragment,
        router: LedgerRouter
    ) = permissionsAskerFactory.create(fragment, router)

    @Provides
    @ScreenScope
    fun provideSelectLedgerPayload(
        signPayloadState: MutableSharedState<SignerPayloadExtrinsic>,
    ): SelectLedgerPayload = SelectLedgerPayload(
        chainId = signPayloadState.getOrThrow().chainId
    )

    @Provides
    @IntoMap
    @ViewModelKey(SignLedgerViewModel::class)
    fun provideViewModel(
        substrateApplication: SubstrateLedgerApplication,
        selectLedgerPayload: SelectLedgerPayload,
        discoveryService: LedgerDeviceDiscoveryService,
        permissionsAsker: PermissionsAsker.Presentation,
        bluetoothManager: BluetoothManager,
        locationManager: LocationManager,
        router: LedgerRouter,
        resourceManager: ResourceManager,
        chainRegistry: ChainRegistry,
        signPayloadState: MutableSharedState<SignerPayloadExtrinsic>,
        extrinsicValidityUseCase: ExtrinsicValidityUseCase,
        selectedAccountUseCase: SelectedAccountUseCase,
        request: SignInterScreenCommunicator.Request,
        interactor: SignLedgerInteractor,
        responder: LedgerSignCommunicator,
    ): ViewModel {
        return SignLedgerViewModel(
            substrateApplication = substrateApplication,
            selectLedgerPayload = selectLedgerPayload,
            discoveryService = discoveryService,
            permissionsAsker = permissionsAsker,
            bluetoothManager = bluetoothManager,
            locationManager = locationManager,
            router = router,
            resourceManager = resourceManager,
            chainRegistry = chainRegistry,
            signPayloadState = signPayloadState,
            extrinsicValidityUseCase = extrinsicValidityUseCase,
            selectedAccountUseCase = selectedAccountUseCase,
            request = request,
            responder = responder,
            interactor = interactor
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): SignLedgerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SignLedgerViewModel::class.java)
    }
}
