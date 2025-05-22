package io.novafoundation.nova.feature_deep_linking.presentation.handling.handlers.accountmigration

import android.net.Uri
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_account_migration.utils.AccountExchangePayload
import io.novafoundation.nova.feature_account_migration.utils.AccountMigrationMixinProvider
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novasama.substrate_sdk_android.extensions.fromHex
import kotlinx.coroutines.flow.MutableSharedFlow

private const val MIGRATION_COMPLETE_PATH = "/migration-complete"

class MigrationCompleteDeepLinkHandler(
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val accountMigrationMixinProvider: AccountMigrationMixinProvider
) : DeepLinkHandler {

    override val callbackFlow = MutableSharedFlow<CallbackEvent>()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false

        return path.startsWith(MIGRATION_COMPLETE_PATH)
    }

    override suspend fun handleDeepLink(data: Uri) {
        automaticInteractionGate.awaitInteractionAllowed()

        val mnemonic = data.getQueryParameter("mnemonic") ?: throw IllegalStateException("No secret was passed")
        val peerPublicKey = data.getQueryParameter("key") ?: throw IllegalStateException("No key was passed")
        val accountName = data.getQueryParameter("name")

        val mixin = accountMigrationMixinProvider.getMixin() ?: throw IllegalStateException("Migration state invalid")
        mixin.onPeerSecretsReceived(
            secret = mnemonic.fromHex(),
            publicKey = peerPublicKey.fromHex(),
            exchangePayload = AccountExchangePayload(accountName)
        )
    }
}
