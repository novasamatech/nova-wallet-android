package io.novafoundation.nova.feature_wallet_connect_impl.di

import android.content.Context
import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.caip.caip2.Caip2Parser
import io.novafoundation.nova.caip.caip2.Caip2Resolver
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.core_db.dao.WalletConnectSessionsDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletMixin
import io.novafoundation.nova.feature_external_sign_api.domain.sign.evm.EvmTypedMessageParser
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
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

    val caip2Parser: Caip2Parser

    val evmTypedMessageParser: EvmTypedMessageParser

    val sessionsDao: WalletConnectSessionsDao

    val selectWalletMixinFactory: SelectWalletMixin.Factory

    val appContext: Context

    val automaticInteractionGate: AutomaticInteractionGate

    fun rootScope(): RootScope
}
