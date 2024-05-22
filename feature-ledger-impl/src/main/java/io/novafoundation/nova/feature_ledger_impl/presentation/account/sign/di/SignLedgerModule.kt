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
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.chainId
import io.novafoundation.nova.common.utils.getOrThrow
import io.novafoundation.nova.common.utils.location.LocationManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.feature_account_api.data.signer.SigningSharedState
import io.novafoundation.nova.feature_account_api.presenatation.sign.LedgerSignCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.domain.account.sign.RealSignLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.sign.SignLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.sign.SignLedgerViewModel
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp.MigrationSubstrateLedgerApplication
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicValidityUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SignLedgerModule {

    @Provides
    @ScreenScope
    fun provideInteractor(chainRegistry: ChainRegistry): SignLedgerInteractor = RealSignLedgerInteractor(chainRegistry)

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
        signPayloadState: SigningSharedState,
    ): SelectLedgerPayload = SelectLedgerPayload(
        chainId = signPayloadState.getOrThrow().extrinsic.chainId
    )

    @Provides
    @ScreenScope
    fun provideMessageFormatter(
        selectLedgerPayload: SelectLedgerPayload,
        factory: LedgerMessageFormatterFactory
    ): LedgerMessageFormatter {
        // TODO legacy for now, replace with mediator with generic & legacy
        return factory.createLegacy(selectLedgerPayload.chainId)
    }

    @Provides
    @IntoMap
    @ViewModelKey(SignLedgerViewModel::class)
    fun provideViewModel(
        substrateApplication: MigrationSubstrateLedgerApplication,
        selectLedgerPayload: SelectLedgerPayload,
        discoveryService: LedgerDeviceDiscoveryService,
        permissionsAsker: PermissionsAsker.Presentation,
        bluetoothManager: BluetoothManager,
        locationManager: LocationManager,
        router: LedgerRouter,
        resourceManager: ResourceManager,
        signPayloadState: SigningSharedState,
        extrinsicValidityUseCase: ExtrinsicValidityUseCase,
        request: SignInterScreenCommunicator.Request,
        interactor: SignLedgerInteractor,
        responder: LedgerSignCommunicator,
        messageFormatter: LedgerMessageFormatter
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
            messageFormatter = messageFormatter,
            signPayloadState = signPayloadState,
            extrinsicValidityUseCase = extrinsicValidityUseCase,
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
