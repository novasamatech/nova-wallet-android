package io.novafoundation.nova.feature_account_impl.data.proxy

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_account_api.data.proxy.MetaAccountsUpdatesRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealMetaAccountsUpdatesRegistry(
    private val preferences: Preferences
) : MetaAccountsUpdatesRegistry {

    private val KEY = "meta_accounts_changes"

    override fun addMetaIds(ids: List<Long>) {
        val metaIdsSet = getUpdates()
            .toMutableSet()
        metaIdsSet.addAll(ids)
        val metaIdsJoinedToString = metaIdsSet.joinToString(",")
        if (metaIdsJoinedToString.isNotEmpty()) {
            preferences.putString(KEY, metaIdsJoinedToString)
        }
    }

    override fun observeUpdates(): Flow<Set<Long>> {
        return preferences.keyFlow(KEY)
            .map { getUpdates() }
    }

    override fun getUpdates(): Set<Long> {
        val metaIds = preferences.getString(KEY)
        if (metaIds.isNullOrEmpty()) return mutableSetOf()

        return metaIds.split(",")
            .map { it.toLong() }
            .toMutableSet()
    }

    override fun remove(ids: List<Long>) {
        val metaIdsSet = getUpdates()
            .toMutableSet()
        metaIdsSet.removeAll(ids)
        val metaIdsJoinedToString = metaIdsSet.joinToString(",")

        if (metaIdsJoinedToString.isEmpty()) {
            preferences.removeField(KEY)
        } else {
            preferences.putString(KEY, metaIdsJoinedToString)
        }
    }

    override fun clear() {
        preferences.removeField(KEY)
    }

    override fun observeUpdatesExist(): Flow<Boolean> {
        return preferences.keyFlow(KEY)
            .map { hasUpdates() }
    }

    override fun hasUpdates(): Boolean {
        return preferences.contains(KEY)
    }
}
