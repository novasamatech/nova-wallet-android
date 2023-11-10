package io.novafoundation.nova.feature_wallet_impl.data.storage

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val TRANSACTIONS_CURSOR_KEY = "TRANSACTIONS_CURSOR_KEY"

// to distinguish between no cursor and null cursor introduce a separate value for `null` cursor.
// Null value in preferences will correspond to `no cursor` state
private const val NULL_CURSOR = "NULL_CURSOR"

class TransferCursorStorage(
    private val preferences: Preferences,
) {

    fun saveCursor(
        chainId: ChainId,
        chainAssetId: Int,
        accountId: AccountId,
        cursor: String?,
    ) {
        val toSave = cursor ?: NULL_CURSOR

        preferences.putString(cursorKey(chainId, chainAssetId, accountId), toSave)
    }
    fun hasCursor(
        chainId: ChainId,
        chainAssetId: Int,
        accountId: AccountId,
    ): Boolean {
        return preferences.contains(cursorKey(chainId, chainAssetId, accountId))
    }

    suspend fun awaitCursor(
        chainId: ChainId,
        chainAssetId: Int,
        accountId: AccountId,
    ) = preferences.stringFlow(cursorKey(chainId, chainAssetId, accountId))
        .filterNotNull() // suspends until cursor is inserted
        .map {
            if (it == NULL_CURSOR) {
                null
            } else {
                it
            }
        }.first()

    private fun cursorKey(chainId: String, chainAssetId: Int, accountId: AccountId): String {
        return "$TRANSACTIONS_CURSOR_KEY:${accountId.toHexString()}:$chainId:$chainAssetId"
    }
}
