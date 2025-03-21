package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.preview.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.generic.preview.PreviewImportGenericLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.generic.preview.RealPreviewImportGenericLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.mappers.LedgerDeviceMapper
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.preview.PreviewImportGenericLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.preview.PreviewImportGenericLedgerViewModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger.SelectLedgerLegacyPayload
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp.GenericSubstrateLedgerApplication
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class PreviewImportGenericLedgerModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        ledgerMigrationTracker: LedgerMigrationTracker,
        genericSubstrateLedgerApplication: GenericSubstrateLedgerApplication,
        ledgerDiscoveryService: LedgerDeviceDiscoveryService
    ): PreviewImportGenericLedgerInteractor {
        return RealPreviewImportGenericLedgerInteractor(ledgerMigrationTracker, genericSubstrateLedgerApplication, ledgerDiscoveryService)
    }

    @Provides
    @ScreenScope
    fun provideMessageFormatter(
        factory: LedgerMessageFormatterFactory,
    ): LedgerMessageFormatter = factory.createGeneric()

    @Provides
    @ScreenScope
    fun provideMessageCommandFormatter(
        messageFormatter: LedgerMessageFormatter,
        messageCommandFormatterFactory: MessageCommandFormatterFactory
    ): MessageCommandFormatter = messageCommandFormatterFactory.create(messageFormatter)

    @Provides
    @IntoMap
    @ViewModelKey(PreviewImportGenericLedgerViewModel::class)
    fun provideViewModel(
        interactor: PreviewImportGenericLedgerInteractor,
        router: LedgerRouter,
        iconGenerator: AddressIconGenerator,
        payload: PreviewImportGenericLedgerPayload,
        externalActions: ExternalActions.Presentation,
        chainRegistry: ChainRegistry,
        resourceManager: ResourceManager,
        deviceMapperFactory: LedgerDeviceMapper,
        messageCommandFormatter: MessageCommandFormatter,
    ): ViewModel {
        return PreviewImportGenericLedgerViewModel(
            interactor = interactor,
            router = router,
            iconGenerator = iconGenerator,
            payload = payload,
            externalActions = externalActions,
            chainRegistry = chainRegistry,
            resourceManager = resourceManager,
            deviceMapperFactory = deviceMapperFactory,
            messageCommandFormatter = messageCommandFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): PreviewImportGenericLedgerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(PreviewImportGenericLedgerViewModel::class.java)
    }
}
