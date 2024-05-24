package io.novafoundation.nova.feature_ledger_impl.di

import coil.ImageLoader
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.location.LocationManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.LedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.signer.SigningSharedState
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicValidityUseCase
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.rpc.RpcCalls

interface LedgerFeatureDependencies {

    val chainRegistry: ChainRegistry

    val appLinksProvider: AppLinksProvider

    val imageLoader: ImageLoader

    val addressIconGenerator: AddressIconGenerator

    val resourceManager: ResourceManager

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    val bluetoothManager: BluetoothManager

    val locationManager: LocationManager

    val permissionAskerFactory: PermissionsAskerFactory

    val contextManager: ContextManager

    val assetSourceRegistry: AssetSourceRegistry

    val tokenRepository: TokenRepository

    val metaAccountDao: MetaAccountDao

    val accountInteractor: AccountInteractor

    val accountRepository: AccountRepository

    val secretStoreV2: SecretStoreV2

    val signSharedState: SigningSharedState

    val extrinsicValidityUseCase: ExtrinsicValidityUseCase

    val selectedAccountUseCase: SelectedAccountUseCase

    val ledgerAddAccountRepository: LedgerAddAccountRepository

    val apiCreator: NetworkApiCreator

    val rpcCalls: RpcCalls

    val metadataShortenerService: MetadataShortenerService
}
