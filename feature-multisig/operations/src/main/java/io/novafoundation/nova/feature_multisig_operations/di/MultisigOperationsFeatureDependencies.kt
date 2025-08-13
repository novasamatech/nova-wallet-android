package io.novafoundation.nova.feature_multisig_operations.di

import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.mixin.copy.CopyTextLauncher
import io.novafoundation.nova.common.data.repository.ToggleFeatureRepository
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.DialogMessageManager
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSplitter
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_api.data.multisig.repository.MultisigOperationLocalCallRepository
import io.novafoundation.nova.feature_account_api.data.multisig.repository.MultisigValidationsRepository
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.LocalIdentity
import io.novafoundation.nova.feature_account_api.domain.account.identity.LocalWithOnChainIdentity
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountUIUseCase
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.MultisigFormatter
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.ProxyFormatter
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.LinkBuilderFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryTokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.di.ExtrinsicSerialization
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.CallTraversal
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface MultisigOperationsFeatureDependencies {

    val assetIconProvider: AssetIconProvider

    val extrinsicSplitter: ExtrinsicSplitter

    val accountRepository: AccountRepository

    val extrinsicService: ExtrinsicService

    val resourceManager: ResourceManager

    val multisigPendingOperationsService: MultisigPendingOperationsService

    val feeLoaderMixinFactory: FeeLoaderMixinV2.Factory

    val imageLoader: ImageLoader

    val externalActions: ExternalActions.Presentation

    val validationExecutor: ValidationExecutor

    val selectedAccountUseCase: SelectedAccountUseCase

    val walletUiUseCase: WalletUiUseCase

    val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper

    val edValidationFactory: EnoughTotalToStayAboveEDValidationFactory

    val assetSourceRegistry: AssetSourceRegistry

    val multisigOperationLocalCallRepository: MultisigOperationLocalCallRepository

    val callTraversal: CallTraversal

    val addressIconGenerator: AddressIconGenerator

    val multisigFormatter: MultisigFormatter

    val proxyFormatter: ProxyFormatter

    val accountInteractor: AccountInteractor

    val arbitraryTokenUseCase: ArbitraryTokenUseCase

    val toggleFeatureRepository: ToggleFeatureRepository

    val actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory

    val multisigValidationsRepository: MultisigValidationsRepository

    val tokenRepository: TokenRepository

    val chainRegistry: ChainRegistry

    val descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher

    val copyTextLauncher: CopyTextLauncher.Presentation

    val accountUIUseCase: AccountUIUseCase

    val linkBuilderFactory: LinkBuilderFactory

    val automaticInteractionGate: AutomaticInteractionGate

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    fun dialogMessageManager(): DialogMessageManager

    @LocalIdentity
    fun localIdentityProvider(): IdentityProvider

    @LocalWithOnChainIdentity
    fun localWithOnChainIdentityProvider(): IdentityProvider

    @ExtrinsicSerialization
    fun extrinsicGson(): Gson
}
