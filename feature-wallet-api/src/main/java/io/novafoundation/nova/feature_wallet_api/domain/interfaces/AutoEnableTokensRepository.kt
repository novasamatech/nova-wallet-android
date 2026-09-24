package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import kotlinx.coroutines.flow.Flow

/**
 * Backs the "Add tokens with positive balance" setting, per wallet.
 *
 * While it is on, a balance turning up in a token nobody has decided about writes that wallet a
 * "show" row. Turning it off does not change what is already on screen and does not stop anything
 * being synced - it only stops new tokens appearing by themselves, so funds arriving in a token the
 * wallet has never shown leave it hidden until the user shows it by hand.
 *
 * Per wallet because the wallets a person keeps are not alike: a main wallet may want everything it
 * receives on screen while a cold one is meant to stay a short, deliberate list.
 */
interface AutoEnableTokensRepository {

    suspend fun autoEnableTokens(metaId: Long): Boolean

    fun autoEnableTokensFlow(metaId: Long): Flow<Boolean>

    suspend fun setAutoEnableTokens(metaId: Long, enabled: Boolean)
}
