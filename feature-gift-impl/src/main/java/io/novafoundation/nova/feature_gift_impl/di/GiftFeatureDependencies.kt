package io.novafoundation.nova.feature_gift_impl.di

import android.content.Context
import coil.ImageLoader
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.IntegrityService
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.core_db.dao.GiftsDao
import io.novafoundation.nova.feature_account_api.data.repository.CreateSecretsRepository
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.LinkBuilderFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.SendUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.EnoughAmountValidatorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.MinAmountFieldValidatorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.GetAssetOptionsMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface GiftFeatureDependencies {

    val amountFormatter: AmountFormatter

    val context: Context

    val preferences: Preferences

    val integrityService: IntegrityService

    val getAssetOptionsMixinFactory: GetAssetOptionsMixin.Factory

    val assetSourceRegistry: AssetSourceRegistry

    val validationExecutor: ValidationExecutor

    val maxActionProviderFactory: MaxActionProviderFactory

    val selectedAccountUseCase: SelectedAccountUseCase

    val enoughAmountValidatorFactory: EnoughAmountValidatorFactory

    val minAmountFieldValidatorFactory: MinAmountFieldValidatorFactory

    val amountChooserMixinFactory: AmountChooserMixin.Factory

    val feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory

    val assetUseCase: ArbitraryAssetUseCase

    val createSecretsRepository: CreateSecretsRepository

    val sendUseCase: SendUseCase

    val walletUiUseCase: WalletUiUseCase

    val externalAccountActions: ExternalActions.Presentation

    val addressIconGenerator: AddressIconGenerator

    val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper

    val encryptionDefaults: EncryptionDefaults

    val encryptedPreferences: EncryptedPreferences

    val linkBuilderFactory: LinkBuilderFactory

    val assetIconProvider: AssetIconProvider

    val tokenFormatter: TokenFormatter

    val fileProvider: FileProvider

    fun giftsDao(): GiftsDao

    fun resourceManager(): ResourceManager

    fun appLinksProvider(): AppLinksProvider

    fun chainRegistry(): ChainRegistry

    fun imageLoader(): ImageLoader

    fun secretStoreV2(): SecretStoreV2

    fun apiCreator(): NetworkApiCreator
}
