package io.novafoundation.nova.feature_swap_impl.presentation.main.di

import androidx.lifecycle.ViewModelStoreOwner
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsState
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_api.presentation.state.DEFAULT_SLIPPAGE
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapTokenSelectionCommunicator
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import io.novafoundation.nova.feature_swap_impl.presentation.main.SwapFormDisplay
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.RealSwapSettingsState
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapSelectionInteractor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_swap_impl.domain.swap.PriceImpactThresholds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_swap_api.presentation.formatters.SwapRateFormatter
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.PriceImpactFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.fieldValidation.LiquidityFieldValidatorFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.fieldValidation.SwapReceiveAmountAboveEDFieldValidatorFactory
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapMaxActionProviderFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.route.SwapRouteFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapOperationPayloadMapper
import io.novafoundation.nova.feature_swap_impl.presentation.main.SwapMainSettingsViewModel
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapAmountInputMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapInputMixinPriceImpactFiatFormatterFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.EnoughAmountValidatorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.GetAssetOptionsMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.feature_wallet_api.data.repository.UsdRateRepository

@Module(includes = [ViewModelModule::class])
class SwapMainSettingsModule {

    @Provides
    @ScreenScope
    fun provideFormState(): SwapSettingsState = RealSwapSettingsState(SwapSettings(slippage = DEFAULT_SLIPPAGE))

    @Provides
    @ScreenScope
    fun provideSwapAmountMixinFactory(
        chainRegistry: ChainRegistry,
        resourceManager: ResourceManager,
        assetIconProvider: AssetIconProvider
    ) = SwapAmountInputMixinFactory(chainRegistry, resourceManager, assetIconProvider)

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
        swapSettingsState: SwapSettingsState,
        selectionInteractor: SwapSelectionInteractor,
        tokenSelectionCommunicator: SwapTokenSelectionCommunicator,
        slippageCommunicator: SlippageSettingsCommunicator,
        display: SwapFormDisplay,
        swapAmountInputMixinFactory: SwapAmountInputMixinFactory,
        chainRegistry: ChainRegistry,
        assetUseCase: ArbitraryAssetUseCase,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        liquidityFieldValidatorFactory: LiquidityFieldValidatorFactory,
        swapReceiveAmountAboveEDFieldValidatorFactory: SwapReceiveAmountAboveEDFieldValidatorFactory,
        payload: SwapSettingsPayload,
        swapInputMixinPriceImpactFiatFormatterFactory: SwapInputMixinPriceImpactFiatFormatterFactory,
        accountUseCase: SelectedAccountUseCase,
        validationExecutor: ValidationExecutor,
        descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
        swapRateFormatter: SwapRateFormatter,
        maxActionProviderFactory: SwapMaxActionProviderFactory,
        operationMapper: SwapOperationPayloadMapper,
        tokenFormatter: TokenFormatter,
        priceImpactFormatter: PriceImpactFormatter,
        priceImpactThresholds: PriceImpactThresholds,
        swapRouteFormatter: SwapRouteFormatter,
        enoughAmountValidatorFactory: EnoughAmountValidatorFactory,
        getAssetOptionsMixinFactory: GetAssetOptionsMixin.Factory,
        analyticsService: AnalyticsService,
        usdRateRepository: UsdRateRepository,
    ): ViewModel {
        return SwapMainSettingsViewModel(
            swapRouter = swapRouter,
            swapInteractor = swapInteractor,
            swapSettingsState = swapSettingsState,
            selectionInteractor = selectionInteractor,
            tokenSelectionCommunicator = tokenSelectionCommunicator,
            slippageCommunicator = slippageCommunicator,
            display = display,
            resourceManager = resourceManager,
            swapAmountInputMixinFactory = swapAmountInputMixinFactory,
            chainRegistry = chainRegistry,
            assetUseCase = assetUseCase,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            payload = payload,
            swapInputMixinPriceImpactFiatFormatterFactory = swapInputMixinPriceImpactFiatFormatterFactory,
            validationExecutor = validationExecutor,
            liquidityFieldValidatorFactory = liquidityFieldValidatorFactory,
            swapReceiveAmountAboveEDFieldValidatorFactory = swapReceiveAmountAboveEDFieldValidatorFactory,
            descriptionBottomSheetLauncher = descriptionBottomSheetLauncher,
            getAssetOptionsMixinFactory = getAssetOptionsMixinFactory,
            swapRateFormatter = swapRateFormatter,
            selectedAccountUseCase = accountUseCase,
            enoughAmountValidatorFactory = enoughAmountValidatorFactory,
            operationMapper = operationMapper,
            tokenFormatter = tokenFormatter,
            priceImpactFormatter = priceImpactFormatter,
            priceImpactThresholds = priceImpactThresholds,
            maxActionProviderFactory = maxActionProviderFactory,
            swapRouteFormatter = swapRouteFormatter,
            analyticsService = analyticsService,
            usdRateRepository = usdRateRepository
        )
    }

    @Provides
    fun provideViewModelCreator(
        owner: ViewModelStoreOwner,
        viewModelFactory: ViewModelProvider.Factory
    ): SwapMainSettingsViewModel {
        return ViewModelProvider(owner, viewModelFactory).get(SwapMainSettingsViewModel::class.java)
    }
}
