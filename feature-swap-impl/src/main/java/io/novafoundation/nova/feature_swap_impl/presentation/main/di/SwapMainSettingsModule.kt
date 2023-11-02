package io.novafoundation.nova.feature_swap_impl.presentation.main.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_swap_impl.data.network.blockhain.updaters.SwapUpdateSystemFactory
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.main.SwapMainSettingsViewModel
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_swap_impl.presentation.common.PriceImpactFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.SwapRateFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.payload.SwapConfirmationPayloadFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.fieldValidation.EnoughAmountToSwapValidatorFactory
import io.novafoundation.nova.feature_swap_impl.presentation.fieldValidation.LiquidityFieldValidatorFactory
import io.novafoundation.nova.feature_swap_impl.presentation.fieldValidation.SwapReceiveAmountAboveEDFieldValidatorFactory
import io.novafoundation.nova.feature_swap_impl.presentation.main.SwapSettingsPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapInputMixinPriceImpactFiatFormatterFactory
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapAmountInputMixinFactory
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SwapMainSettingsModule {

    @Provides
    @ScreenScope
    fun provideSwapAmountMixinFactory(
        chainRegistry: ChainRegistry,
        resourceManager: ResourceManager
    ) = SwapAmountInputMixinFactory(chainRegistry, resourceManager)

    @Provides
    @ScreenScope
    fun provideSwapInputMixinPriceImpactFiatFormatterFactory(
        priceImpactFormatter: PriceImpactFormatter
    ) = SwapInputMixinPriceImpactFiatFormatterFactory(priceImpactFormatter)

    @Provides
    @ScreenScope
    fun provideLiquidityFieldValidatorFactory(resourceManager: ResourceManager): LiquidityFieldValidatorFactory {
        return LiquidityFieldValidatorFactory(resourceManager)
    }

    @Provides
    @ScreenScope
    fun provideEnoughAmountToSwapValidatorFactory(resourceManager: ResourceManager): EnoughAmountToSwapValidatorFactory {
        return EnoughAmountToSwapValidatorFactory(resourceManager)
    }

    @Provides
    @ScreenScope
    fun provideSwapAmountAboveEDFieldValidatorFactory(
        resourceManager: ResourceManager,
        chainRegistry: ChainRegistry,
        assetSourceRegistry: AssetSourceRegistry
    ): SwapReceiveAmountAboveEDFieldValidatorFactory {
        return SwapReceiveAmountAboveEDFieldValidatorFactory(resourceManager, chainRegistry, assetSourceRegistry)
    }

    @Provides
    @IntoMap
    @ViewModelKey(SwapMainSettingsViewModel::class)
    fun provideViewModel(
        swapRouter: SwapRouter,
        swapInteractor: SwapInteractor,
        resourceManager: ResourceManager,
        swapSettingsStateProvider: SwapSettingsStateProvider,
        swapAmountInputMixinFactory: SwapAmountInputMixinFactory,
        chainRegistry: ChainRegistry,
        assetUseCase: ArbitraryAssetUseCase,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        liquidityFieldValidatorFactory: LiquidityFieldValidatorFactory,
        enoughAmountToSwapValidatorFactory: EnoughAmountToSwapValidatorFactory,
        swapReceiveAmountAboveEDFieldValidatorFactory: SwapReceiveAmountAboveEDFieldValidatorFactory,
        payload: SwapSettingsPayload,
        swapUpdateSystemFactory: SwapUpdateSystemFactory,
        swapInputMixinPriceImpactFiatFormatterFactory: SwapInputMixinPriceImpactFiatFormatterFactory,
        validationExecutor: ValidationExecutor,
        swapRateFormatter: SwapRateFormatter,
        swapConfirmationPayloadFormatter: SwapConfirmationPayloadFormatter
    ): ViewModel {
        return SwapMainSettingsViewModel(
            swapRouter = swapRouter,
            swapInteractor = swapInteractor,
            swapSettingsStateProvider = swapSettingsStateProvider,
            resourceManager = resourceManager,
            swapAmountInputMixinFactory = swapAmountInputMixinFactory,
            chainRegistry = chainRegistry,
            assetUseCase = assetUseCase,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            actionAwaitableFactory = actionAwaitableMixinFactory,
            payload = payload,
            swapUpdateSystemFactory = swapUpdateSystemFactory,
            swapInputMixinPriceImpactFiatFormatterFactory = swapInputMixinPriceImpactFiatFormatterFactory,
            validationExecutor = validationExecutor,
            liquidityFieldValidatorFactory = liquidityFieldValidatorFactory,
            enoughAmountToSwapValidatorFactory = enoughAmountToSwapValidatorFactory,
            swapReceiveAmountAboveEDFieldValidatorFactory = swapReceiveAmountAboveEDFieldValidatorFactory,
            swapRateFormatter = swapRateFormatter,
            swapConfirmationPayloadFormatter = swapConfirmationPayloadFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SwapMainSettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SwapMainSettingsViewModel::class.java)
    }
}
