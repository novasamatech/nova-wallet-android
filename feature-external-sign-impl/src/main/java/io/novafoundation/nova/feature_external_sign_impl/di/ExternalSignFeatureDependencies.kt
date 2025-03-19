package io.novafoundation.nova.feature_external_sign_impl.di

import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.di.ExtrinsicSerialization
import io.novafoundation.nova.runtime.ethereum.gas.GasPriceProviderFactory
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import okhttp3.OkHttpClient

interface ExternalSignFeatureDependencies {

    fun currencyRepository(): CurrencyRepository

    fun accountRepository(): AccountRepository

    fun resourceManager(): ResourceManager

    fun selectedAccountUseCase(): SelectedAccountUseCase

    fun addressIconGenerator(): AddressIconGenerator

    fun chainRegistry(): ChainRegistry

    fun imageLoader(): ImageLoader

    fun extrinsicService(): ExtrinsicService

    fun tokenRepository(): TokenRepository

    fun secretStoreV2(): SecretStoreV2

    @ExtrinsicSerialization
    fun extrinsicGson(): Gson

    val feeLoaderMixinFactory: FeeLoaderMixinV2.Factory

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    val walletUiUseCase: WalletUiUseCase

    val okHttpClient: OkHttpClient

    val walletRepository: WalletRepository

    val validationExecutor: ValidationExecutor

    val signerProvider: SignerProvider

    val gasPriceProviderFactory: GasPriceProviderFactory

    val rpcCalls: RpcCalls

    val metadataShortenerService: MetadataShortenerService
}
