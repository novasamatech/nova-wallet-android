package io.novafoundation.nova.feature_account_impl.presentation.account.details.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.format.AddressSchemeFormatter
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.modules.Caching
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.ProxyFormatter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.WalletDetailsViewModel
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.WalletDetailsMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.AccountFormatterFactory
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.AddAccountLauncherPresentationFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class AccountDetailsModule {

    @Provides
    fun provideAccountFormatterFactory(
        resourceManager: ResourceManager,
        @Caching iconGenerator: AddressIconGenerator,
    ): AccountFormatterFactory {
        return AccountFormatterFactory(iconGenerator, resourceManager)
    }

    @Provides
    fun provideWalletDetailsMixinFactory(
        polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
        resourceManager: ResourceManager,
        accountFormatterFactory: AccountFormatterFactory,
        proxyFormatter: ProxyFormatter,
        interactor: WalletDetailsInteractor,
        appLinksProvider: AppLinksProvider,
        ledgerMigrationTracker: LedgerMigrationTracker,
        router: AccountRouter,
        addressSchemeFormatter: AddressSchemeFormatter
    ): WalletDetailsMixinFactory {
        return WalletDetailsMixinFactory(
            polkadotVaultVariantConfigProvider,
            resourceManager,
            accountFormatterFactory,
            proxyFormatter,
            interactor,
            appLinksProvider,
            ledgerMigrationTracker,
            router,
            addressSchemeFormatter
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(WalletDetailsViewModel::class)
    fun provideViewModel(
        rootScope: RootScope,
        interactor: WalletDetailsInteractor,
        router: AccountRouter,
        metaId: Long,
        externalActions: ExternalActions.Presentation,
        chainRegistry: ChainRegistry,
        importTypeChooserMixin: ImportTypeChooserMixin.Presentation,
        addAccountLauncherPresentationFactory: AddAccountLauncherPresentationFactory,
        walletDetailsMixinFactory: WalletDetailsMixinFactory
    ): ViewModel {
        return WalletDetailsViewModel(
            rootScope = rootScope,
            interactor = interactor,
            accountRouter = router,
            metaId = metaId,
            externalActions = externalActions,
            chainRegistry = chainRegistry,
            importTypeChooserMixin = importTypeChooserMixin,
            addAccountLauncherPresentationFactory = addAccountLauncherPresentationFactory,
            walletDetailsMixinFactory = walletDetailsMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): WalletDetailsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(WalletDetailsViewModel::class.java)
    }
}
