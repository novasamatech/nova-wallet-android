package io.novafoundation.nova.feature_account_migration.utils

import io.novafoundation.nova.common.utils.invokeOnCompletion
import io.novafoundation.nova.feature_account_migration.utils.common.ExchangeSecretsMixin
import io.novafoundation.nova.feature_account_migration.utils.common.KeyExchangeUtils
import io.novafoundation.nova.feature_account_migration.utils.common.RealExchangeSecretsMixin
import kotlinx.coroutines.CoroutineScope

class AccountMigrationMixinProvider(
    private val keyExchangeUtils: KeyExchangeUtils
) {

    private var exchangeSecretsMixin: ExchangeSecretsMixin<AccountExchangePayload>? = null

    fun getMixin(): ExchangeSecretsMixin<AccountExchangePayload>? = exchangeSecretsMixin

    fun createAndBindWithScope(coroutineScope: CoroutineScope): ExchangeSecretsMixin<AccountExchangePayload> {
        return RealExchangeSecretsMixin<AccountExchangePayload>(
            keyExchangeUtils,
            secretProvider = { throw InterruptedException("No supported secret exchange") },
            exchangePayloadProvider = { throw InterruptedException("No supported secret exchange") },
            coroutineScope
        ).apply {
            exchangeSecretsMixin = this

            coroutineScope.invokeOnCompletion {
                exchangeSecretsMixin = null
            }
        }
    }
}
