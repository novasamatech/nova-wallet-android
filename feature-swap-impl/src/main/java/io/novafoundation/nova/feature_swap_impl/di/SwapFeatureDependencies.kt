package io.novafoundation.nova.feature_swap_impl.di

import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.common.view.input.chooser.ListChooserMixin
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.fee.capability.CustomFeeCapabilityFacade
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.identity.IdentityMixin
import io.novafoundation.nova.feature_buy_api.domain.BuyTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixin
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixinUi
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.paths.PathQuoter
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuoting
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.XcmVersionDetector
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

interface SwapFeatureDependencies {

    val validationExecutor: ValidationExecutor

    val preferences: Preferences

    val walletRepository: WalletRepository

    val chainRegistry: ChainRegistry

    val imageLoader: ImageLoader

    val addressIconGenerator: AddressIconGenerator

    val resourceManager: ResourceManager

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    val tokenRepository: TokenRepository

    val accountRepository: AccountRepository

    val selectedAccountUseCase: SelectedAccountUseCase

    val storageCache: StorageCache

    val externalAccountActions: ExternalActions.Presentation

    val amountMixinFactory: AmountChooserMixin.Factory

    val extrinsicServiceFactory: ExtrinsicService.Factory

    val resourceHintsMixinFactory: ResourcesHintsMixinFactory

    val walletUiUseCase: WalletUiUseCase

    val computationalCache: ComputationalCache

    val networkApiCreator: NetworkApiCreator

    @Named(REMOTE_STORAGE_SOURCE)
    fun remoteStorageDataSource(): StorageDataSource

    val onChainIdentityRepository: OnChainIdentityRepository

    val listChooserMixinFactory: ListChooserMixin.Factory

    val identityMixinFactory: IdentityMixin.Factory

    val storageStorageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory

    val hydraDxAssetIdConverter: HydraDxAssetIdConverter

    val hydraDxQuotingFactory: HydraDxQuoting.Factory

    val runtimeCallsApi: MultiChainRuntimeCallsApi

    val assetUseCase: ArbitraryAssetUseCase

    val assetSourceRegistry: AssetSourceRegistry

    val chainStateRepository: ChainStateRepository

    val descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher

    val crossChainTransfersRepository: CrossChainTransfersRepository

    val buyTokenRegistry: BuyTokenRegistry

    val buyMixinFactory: BuyMixin.Factory

    val buyMixinUi: BuyMixinUi

    val crossChainTransfersUseCase: CrossChainTransfersUseCase

    val operationDao: OperationDao

    val multiLocationConverterFactory: MultiLocationConverterFactory

    val gson: Gson

    val customFeeCapabilityFacade: CustomFeeCapabilityFacade

    val quoterFactory: PathQuoter.Factory

    val hydrationFeeInjector: HydrationFeeInjector

    val defaultFeePaymentRegistry: FeePaymentProviderRegistry

    val feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory

    val assetIconProvider: AssetIconProvider

    val assetsValidationContextFactory: AssetsValidationContext.Factory

    val xcmVersionDetector: XcmVersionDetector
}
