package io.novafoundation.nova.feature_assets.data.repository.defaultTokens

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PREF_HAS_USED_LOAD_MORE = "PREF_HAS_USED_LOAD_MORE"
private const val PREF_USER_ADDED_TOKENS = "PREF_USER_ADDED_TOKENS"

class LoadMoreTokensPreferences(
    private val preferences: Preferences
) {

    fun hasUsedLoadMore(): Boolean {
        return preferences.getBoolean(PREF_HAS_USED_LOAD_MORE, false)
    }

    fun setHasUsedLoadMore(value: Boolean) {
        preferences.putBoolean(PREF_HAS_USED_LOAD_MORE, value)
    }

    fun hasUsedLoadMoreFlow(): Flow<Boolean> {
        return preferences.booleanFlow(PREF_HAS_USED_LOAD_MORE, false)
    }

    fun getUserAddedTokens(): Set<FullChainAssetId> {
        return preferences.getStringSet(PREF_USER_ADDED_TOKENS)
            .mapNotNull { deserializeFullChainAssetId(it) }
            .toSet()
    }

    fun userAddedTokensFlow(): Flow<Set<FullChainAssetId>> {
        return preferences.stringSetFlow(PREF_USER_ADDED_TOKENS) { emptySet() }
            .map { stringSet ->
                stringSet?.mapNotNull { deserializeFullChainAssetId(it) }?.toSet() ?: emptySet()
            }
    }

    fun addUserAddedToken(id: FullChainAssetId) {
        val current = preferences.getStringSet(PREF_USER_ADDED_TOKENS).toMutableSet()
        current.add(serializeFullChainAssetId(id))
        preferences.putStringSet(PREF_USER_ADDED_TOKENS, current)
    }

    fun removeUserAddedToken(id: FullChainAssetId) {
        val current = preferences.getStringSet(PREF_USER_ADDED_TOKENS).toMutableSet()
        current.remove(serializeFullChainAssetId(id))
        preferences.putStringSet(PREF_USER_ADDED_TOKENS, current)
    }

    private fun serializeFullChainAssetId(id: FullChainAssetId): String {
        return "${id.chainId}:${id.assetId}"
    }

    private fun deserializeFullChainAssetId(value: String): FullChainAssetId? {
        val parts = value.split(":", limit = 2)
        if (parts.size != 2) return null
        val assetId = parts[1].toIntOrNull() ?: return null
        return FullChainAssetId(parts[0], assetId)
    }
}
