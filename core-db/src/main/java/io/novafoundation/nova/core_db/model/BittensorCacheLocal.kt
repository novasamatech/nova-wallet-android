package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Parsed and validated payloads only. Keys contain hashes, never account addresses or credentials. */
@Entity(tableName = "bittensor_cache")
data class BittensorCacheLocal(
    @PrimaryKey val key: String,
    val namespace: String,
    val resource: String,
    val account: String,
    val payload: String,
    val fetchedAt: Long,
    val expiresAt: Long,
    val generationAsOf: String?,
    val generationBlock: String?
)
