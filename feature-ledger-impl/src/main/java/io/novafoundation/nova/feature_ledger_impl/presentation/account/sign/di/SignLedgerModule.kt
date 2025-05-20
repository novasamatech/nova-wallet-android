package io.novafoundation.nova.feature_ledger_impl.presentation.account.sign.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.modules.shared.PermissionAskerForFragmentModule
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.chainId
import io.novafoundation.nova.common.utils.getOrThrow
import io.novafoundation.nova.common.utils.location.LocationManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_account_api.data.signer.SigningSharedState
import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_account_api.presenatation.sign.LedgerSignCommunicator
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.domain.account.sign.RealSignLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.sign.SignLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.migration.LedgerMigrationUseCase
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.mappers.LedgerDeviceFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.sign.SignLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.sign.SignLedgerViewModel
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicValidityUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class, PermissionAskerForFragmentModule::class])
class SignLedgerModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        chainRegistry: ChainRegistry,
        signLedgerPayload: SignLedgerPayload,
        migrationUseCase: LedgerMigrationUseCase,
    ): SignLedgerInteractor = RealSignLedgerInteractor(
        chainRegistry = chainRegistry,
        usedVariant = signLedgerPayload.ledgerVariant,
        migrationUseCase = migrationUseCase,
    )

    @Provides
    @ScreenScope
    fun provideMessageFormatter(
        signPayloadState: SigningSharedState,
        signLedgerPayload: SignLedgerPayload,
        factory: LedgerMessageFormatterFactory
    ): LedgerMessageFormatter {
        val chainId = signPayloadState.getOrThrow().extrinsic.chainId

        return when (signLedgerPayload.ledgerVariant) {
            LedgerVariant.LEGACY -> factory.createLegacy(chainId, showAlerts = true)
            LedgerVariant.GENERIC -> factory.createGeneric()
        }
    }

    @Provides
    @ScreenScope
    fun provideMessageCommandFormatter(
        messageFormatter: LedgerMessageFormatter,
        messageCommandFormatterFactory: MessageCommandFormatterFactory
    ): MessageCommandFormatter = messageCommandFormatterFactory.create(messageFormatter)

    @Provides
    @IntoMap
    @ViewModelKey(SignLedgerViewModel::class)
    fun provideViewModel(
        discoveryService: LedgerDeviceDiscoveryService,
        permissionsAsker: PermissionsAsker.Presentation,
        bluetoothManager: BluetoothManager,
        locationManager: LocationManager,
        router: LedgerRouter,
        resourceManager: ResourceManager,
        signPayloadState: SigningSharedState,
        extrinsicValidityUseCase: ExtrinsicValidityUseCase,
        payload: SignLedgerPayload,
        interactor: SignLedgerInteractor,
        responder: LedgerSignCommunicator,
        messageFormatter: LedgerMessageFormatter,
        deviceMapperFactory: LedgerDeviceFormatter,
        messageCommandFormatter: MessageCommandFormatter
    ): ViewModel {
        return SignLedgerViewModel(
            discoveryService = discoveryService,
            permissionsAsker = permissionsAsker,
            bluetoothManager = bluetoothManager,
            locationManager = locationManager,
            router = router,
            resourceManager = resourceManager,
            messageFormatter = messageFormatter,
            signPayloadState = signPayloadState,
            extrinsicValidityUseCase = extrinsicValidityUseCase,
            payload = payload,
            responder = responder,
            interactor = interactor,
            ledgerDeviceFormatter = deviceMapperFactory,
            messageCommandFormatter = messageCommandFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): SignLedgerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SignLedgerViewModel::class.java)
    }
}
