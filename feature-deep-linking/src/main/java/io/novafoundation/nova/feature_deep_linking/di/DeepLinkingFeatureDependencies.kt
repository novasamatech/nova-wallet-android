package io.novafoundation.nova.feature_deep_linking.di

import android.content.Context
import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_deep_link_building.presentation.AssetDetailsDeepLinkConfigurator
import io.novafoundation.nova.feature_deep_link_building.presentation.ReferendumDetailsDeepLinkConfigurator
import io.novafoundation.nova.feature_governance_api.data.MutableGovernanceState
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.presentation.referenda.common.ReferendaStatusFormatter
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectService
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface DeepLinkingFeatureDependencies {

    val rootScope: RootScope

    val preferences: Preferences

    val context: Context

    val chainRegistry: ChainRegistry

    val permissionsAskerFactory: PermissionsAskerFactory

    val resourceManager: ResourceManager

    val accountRepository: AccountRepository

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    val governanceSourceRegistry: GovernanceSourceRegistry

    val imageLoader: ImageLoader

    val gson: Gson

    val referendaStatusFormatter: ReferendaStatusFormatter

    val automaticInteractionGate: AutomaticInteractionGate

    val dAppMetadataRepository: DAppMetadataRepository

    val mutableGovernanceState: MutableGovernanceState

    val encryptionDefaults: EncryptionDefaults

    val walletConnectService: WalletConnectService

    val referendumDetailsDeepLinkConfigurator: ReferendumDetailsDeepLinkConfigurator

    val assetDetailsDeepLinkConfigurator: AssetDetailsDeepLinkConfigurator
}
