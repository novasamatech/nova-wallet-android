package io.novafoundation.nova.feature_deep_linking.presentation.handling.handlers.accountmigration

import android.net.Uri
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_migration.utils.AccountExchangePayload
import io.novafoundation.nova.feature_account_migration.utils.AccountMigrationMixinProvider
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novasama.substrate_sdk_android.extensions.fromHex
import kotlinx.coroutines.flow.MutableSharedFlow

private const val MIGRATION_COMPLETE_PATH = "/migration-complete"

class MigrationCompleteDeepLinkHandler(
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val accountMigrationMixinProvider: AccountMigrationMixinProvider,
    private val repository: AccountRepository
) : DeepLinkHandler {

    override val callbackFlow = MutableSharedFlow<CallbackEvent>()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false

        return path.startsWith(MIGRATION_COMPLETE_PATH)
    }

    override suspend fun handleDeepLink(data: Uri) {
        if (repository.isAccountSelected()) {
            automaticInteractionGate.awaitInteractionAllowed()
        }

        val mnemonic = data.getQueryParameter("mnemonic") ?: error("No secret was passed")
        val peerPublicKey = data.getQueryParameter("key") ?: error("No key was passed")
        val accountName = data.getQueryParameter("name")

        val mixin = accountMigrationMixinProvider.getMixin() ?: error("Migration state invalid")
        mixin.onPeerSecretsReceived(
            secret = mnemonic.fromHex(),
            publicKey = peerPublicKey.fromHex(),
            exchangePayload = AccountExchangePayload(accountName)
        )
    }
}
