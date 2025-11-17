package io.novafoundation.nova.feature_gift_impl.presentation.confirm.di

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
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_gift_impl.domain.CreateGiftInteractor
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_gift_impl.presentation.confirm.CreateGiftConfirmPayload
import io.novafoundation.nova.feature_gift_impl.presentation.confirm.CreateGiftConfirmViewModel
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class CreateGiftConfirmModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): CreateGiftConfirmViewModel {
        return ViewModelProvider(fragment, factory).get(CreateGiftConfirmViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(CreateGiftConfirmViewModel::class)
    fun provideViewModel(
        router: GiftRouter,
        chainRegistry: ChainRegistry,
        validationExecutor: ValidationExecutor,
        assetUseCase: ArbitraryAssetUseCase,
        walletUiUseCase: WalletUiUseCase,
        payload: CreateGiftConfirmPayload,
        amountFormatter: AmountFormatter,
        resourceManager: ResourceManager,
        externalActions: ExternalActions.Presentation,
        createGiftInteractor: CreateGiftInteractor,
        addressIconGenerator: AddressIconGenerator,
        selectedAccountUseCase: SelectedAccountUseCase,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    ): ViewModel {
        return CreateGiftConfirmViewModel(
            router = router,
            chainRegistry = chainRegistry,
            validationExecutor = validationExecutor,
            assetUseCase = assetUseCase,
            walletUiUseCase = walletUiUseCase,
            payload = payload,
            amountFormatter = amountFormatter,
            resourceManager = resourceManager,
            externalActions = externalActions,
            createGiftInteractor = createGiftInteractor,
            addressIconGenerator = addressIconGenerator,
            selectedAccountUseCase = selectedAccountUseCase,
            feeLoaderMixinFactory = feeLoaderMixinFactory
        )
    }
}
