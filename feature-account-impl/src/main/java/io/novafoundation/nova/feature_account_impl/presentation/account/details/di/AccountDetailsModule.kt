package io.novafoundation.nova.feature_account_impl.presentation.account.details.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.modules.Caching
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.AccountDetailsViewModel
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.AddAccountLauncherMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class AccountDetailsModule {

    @Provides
    @IntoMap
    @ViewModelKey(AccountDetailsViewModel::class)
    fun provideViewModel(
        interactor: AccountDetailsInteractor,
        router: AccountRouter,
        resourceManager: ResourceManager,
        @Caching iconGenerator: AddressIconGenerator,
        metaId: Long,
        externalActions: ExternalActions.Presentation,
        chainRegistry: ChainRegistry,
        importTypeChooserMixin: ImportTypeChooserMixin.Presentation,
        addAccountLauncherMixin: AddAccountLauncherMixin.Presentation,
        polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    ): ViewModel {
        return AccountDetailsViewModel(
            interactor = interactor,
            accountRouter = router,
            iconGenerator = iconGenerator,
            resourceManager = resourceManager,
            metaId = metaId,
            externalActions = externalActions,
            chainRegistry = chainRegistry,
            importTypeChooserMixin = importTypeChooserMixin,
            addAccountLauncherMixin = addAccountLauncherMixin,
            polkadotVaultVariantConfigProvider = polkadotVaultVariantConfigProvider
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): AccountDetailsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AccountDetailsViewModel::class.java)
    }
}
