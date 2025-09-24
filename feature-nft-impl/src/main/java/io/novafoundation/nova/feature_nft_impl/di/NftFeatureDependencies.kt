package io.novafoundation.nova.feature_nft_impl.di

import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

interface NftFeatureDependencies {

    val amountFormatter: AmountFormatter

    fun accountRepository(): AccountRepository

    fun resourceManager(): ResourceManager

    fun selectedAccountUseCase(): SelectedAccountUseCase

    fun addressIconGenerator(): AddressIconGenerator

    fun gson(): Gson

    fun chainRegistry(): ChainRegistry

    fun imageLoader(): ImageLoader

    fun externalAccountActions(): ExternalActions.Presentation

    fun addressDisplayUseCase(): AddressDisplayUseCase

    fun feeLoaderMixinFactory(): FeeLoaderMixin.Factory

    fun extrinsicService(): ExtrinsicService

    fun tokenRepository(): TokenRepository

    fun apiCreator(): NetworkApiCreator

    fun nftDao(): NftDao

    @Named(REMOTE_STORAGE_SOURCE)
    fun remoteStorageSource(): StorageDataSource

    fun exceptionHandler(): HttpExceptionHandler
}
