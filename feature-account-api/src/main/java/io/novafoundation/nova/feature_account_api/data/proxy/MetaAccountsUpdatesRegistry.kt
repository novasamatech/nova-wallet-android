package io.novafoundation.nova.feature_account_api.data.proxy

import kotlinx.coroutines.flow.Flow

interface MetaAccountsUpdatesRegistry {

    fun addMetaIds(ids: List<Long>)

    fun observeUpdates(): Flow<Set<Long>>

    fun getUpdates(): Set<Long>

    fun remove(ids: List<Long>)

    fun clear()

    fun hasUpdates(): Boolean

    fun observeUpdatesExist(): Flow<Boolean>

    fun observeConsumedUpdatesMetaIds(): Flow<Set<Long>>
}
