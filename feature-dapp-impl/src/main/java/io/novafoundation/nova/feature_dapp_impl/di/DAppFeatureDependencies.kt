package io.novafoundation.nova.feature_dapp_impl.di

import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.di.ExtrinsicSerialization
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface DAppFeatureDependencies {

    fun accountRepository(): AccountRepository

    fun resourceManager(): ResourceManager

    fun appLinksProvider(): AppLinksProvider

    fun selectedAccountUseCase(): SelectedAccountUseCase

    fun addressIconGenerator(): AddressIconGenerator

    fun gson(): Gson

    fun chainRegistry(): ChainRegistry

    fun imageLoader(): ImageLoader

    fun feeLoaderMixinFactory(): FeeLoaderMixin.Factory

    fun extrinsicService(): ExtrinsicService

    fun tokenRepository(): TokenRepository

    fun secretStoreV2(): SecretStoreV2

    @ExtrinsicSerialization
    fun extrinsicGson(): Gson

    fun apiCreator(): NetworkApiCreator
}
