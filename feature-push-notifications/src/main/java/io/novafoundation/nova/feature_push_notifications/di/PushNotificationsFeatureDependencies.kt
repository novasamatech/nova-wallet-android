package io.novafoundation.nova.feature_push_notifications.di

import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator
import android.content.Context
import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.common.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.interfaces.ActivityIntentProvider
import io.novafoundation.nova.common.interfaces.BuildTypeProvider
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigApprovalsRepository
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.LocalWithOnChainIdentity
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.presentation.balance.detail.deeplink.AssetDetailsDeepLinkConfigurator
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.presentation.referenda.common.ReferendaStatusFormatter
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.deeplink.configurators.ReferendumDetailsDeepLinkConfigurator
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallFormatter
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface PushNotificationsFeatureDependencies {

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

    val assetDetailsDeepLinkConfigurator: AssetDetailsDeepLinkConfigurator

    val tokenRepository: TokenRepository

    val provideActivityIntentProvider: ActivityIntentProvider

    val appLinksProvider: AppLinksProvider

    val referendumDetailsDeepLinkConfigurator: ReferendumDetailsDeepLinkConfigurator

    val multisigOperationDeepLinkConfigurator: MultisigOperationDeepLinkConfigurator

    val metaAccountChangesEventBus: MetaAccountChangesEventBus

    val googleApiAvailabilityProvider: GoogleApiAvailabilityProvider

    val multisigCallFormatter: MultisigCallFormatter

    val multisigApprovalsRepository: MultisigApprovalsRepository

    fun buildTypeProvider(): BuildTypeProvider

    @LocalWithOnChainIdentity
    fun localWithOnChainIdentityProvider(): IdentityProvider
}
