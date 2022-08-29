package io.novafoundation.nova.feature_ledger_impl.di

import coil.ImageLoader
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface LedgerFeatureDependencies {

    val chainRegistry: ChainRegistry

    val appLinksProvider: AppLinksProvider

    val imageLoader: ImageLoader

    val addressIconGenerator: AddressIconGenerator

    val resourceManager: ResourceManager

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    val bluetoothManager: BluetoothManager

    val permissionAskerFactory: PermissionsAskerFactory

    val contextManager: ContextManager

    val assetSourceRegistry: AssetSourceRegistry

    val tokenRepository: TokenRepository

    val metaAccountDao: MetaAccountDao

    val accountInteractor: AccountInteractor

    val accountRepository: AccountRepository
}
