package io.novafoundation.nova.feature_swap_impl.di

import coil.ImageLoader
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.view.input.chooser.ListChooserMixin
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.identity.IdentityMixin
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

interface SwapFeatureDependencies {

    val feeLoaderMixinFactory: FeeLoaderMixin.Factory

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

    val extrinsicService: ExtrinsicService

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
}
