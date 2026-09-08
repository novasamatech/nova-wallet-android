package io.novafoundation.nova.common.data.repository

import io.novafoundation.nova.common.data.storage.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Backs the "Add tokens with positive balance" setting: while it is on, a token the wallet holds
 * shows up on its own instead of having to be found in Manage tokens.
 */
interface AutoEnableTokensRepository {

    suspend fun autoEnableTokens(): Boolean

    fun autoEnableTokensFlow(): Flow<Boolean>

    suspend fun setAutoEnableTokens(enabled: Boolean)
}

private const val PREFS_AUTO_ENABLE_TOKENS = "PREFS_AUTO_ENABLE_TOKENS"

// On by default: it only ever adds tokens the wallet actually holds, and anything the user hid
// on purpose is protected by ChainAssetLocal.enabledOverriddenByUser.
private const val AUTO_ENABLE_TOKENS_DEFAULT = true

class RealAutoEnableTokensRepository(
    private val preferences: Preferences
) : AutoEnableTokensRepository {

    override suspend fun autoEnableTokens(): Boolean = withContext(Dispatchers.IO) {
        preferences.getBoolean(PREFS_AUTO_ENABLE_TOKENS, AUTO_ENABLE_TOKENS_DEFAULT)
    }

    override fun autoEnableTokensFlow(): Flow<Boolean> {
        return preferences.booleanFlow(PREFS_AUTO_ENABLE_TOKENS, AUTO_ENABLE_TOKENS_DEFAULT)
    }

    override suspend fun setAutoEnableTokens(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferences.putBoolean(PREFS_AUTO_ENABLE_TOKENS, enabled)
    }
}
