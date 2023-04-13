package io.novafoundation.nova.feature_wallet_connect_impl.di

import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.caip.caip2.Caip2Resolver
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface WalletConnectFeatureDependencies {

    val accountRepository: AccountRepository

    val resourceManager: ResourceManager

    val selectedAccountUseCase: SelectedAccountUseCase

    val addressIconGenerator: AddressIconGenerator

    val gson: Gson

    val chainRegistry: ChainRegistry

    val imageLoader: ImageLoader

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    val walletUiUseCase: WalletUiUseCase

    val permissionsAskerFactory: PermissionsAskerFactory

    val caip2Resolver: Caip2Resolver
}
