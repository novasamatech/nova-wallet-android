package io.novafoundation.nova.feature_gift_impl.presentation.amount.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_gift_impl.domain.CreateGiftInteractor
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_gift_impl.presentation.amount.GiftMinAmountProviderFactory
import io.novafoundation.nova.feature_gift_impl.presentation.amount.SelectGiftAmountPayload
import io.novafoundation.nova.feature_gift_impl.presentation.amount.SelectGiftAmountViewModel
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.EnoughAmountValidatorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.MinAmountFieldValidatorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.GetAssetOptionsMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SelectGiftAmountModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): SelectGiftAmountViewModel {
        return ViewModelProvider(fragment, factory).get(SelectGiftAmountViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(SelectGiftAmountViewModel::class)
    fun provideViewModel(
        router: GiftRouter,
        chainRegistry: ChainRegistry,
        validationExecutor: ValidationExecutor,
        assetUseCase: ArbitraryAssetUseCase,
        payload: SelectGiftAmountPayload,
        maxActionProviderFactory: MaxActionProviderFactory,
        amountFormatter: AmountFormatter,
        resourceManager: ResourceManager,
        getAssetOptionsMixinFactory: GetAssetOptionsMixin.Factory,
        createGiftInteractor: CreateGiftInteractor,
        selectedAccountUseCase: SelectedAccountUseCase,
        enoughAmountValidatorFactory: EnoughAmountValidatorFactory,
        minAmountFieldValidatorFactory: MinAmountFieldValidatorFactory,
        giftMinAmountProviderFactory: GiftMinAmountProviderFactory,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    ): ViewModel {
        return SelectGiftAmountViewModel(
            router = router,
            chainRegistry = chainRegistry,
            validationExecutor = validationExecutor,
            assetUseCase = assetUseCase,
            payload = payload,
            maxActionProviderFactory = maxActionProviderFactory,
            amountFormatter = amountFormatter,
            resourceManager = resourceManager,
            getAssetOptionsMixinFactory = getAssetOptionsMixinFactory,
            createGiftInteractor = createGiftInteractor,
            selectedAccountUseCase = selectedAccountUseCase,
            enoughAmountValidatorFactory = enoughAmountValidatorFactory,
            minAmountFieldValidatorFactory = minAmountFieldValidatorFactory,
            giftMinAmountProviderFactory = giftMinAmountProviderFactory,
            amountChooserMixinFactory = amountChooserMixinFactory,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
        )
    }
}
