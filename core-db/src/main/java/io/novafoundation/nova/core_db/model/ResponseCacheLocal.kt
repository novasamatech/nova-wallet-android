package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal

/** Complete resource identity. Null owner is reserved for public, account-independent data. */
data class ResponseCacheKey(val normalizedUrl: String, val metaAccountId: Long?) {
    val storageKey: String get() = "${metaAccountId?.toString() ?: "public"}|$normalizedUrl"
}

@Entity(
    tableName = "response_cache",
    foreignKeys = [
        ForeignKey(
            entity = MetaAccountLocal::class,
            parentColumns = ["id"],
            childColumns = ["metaAccountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["metaAccountId"])]
)
data class ResponseCacheLocal(
    /** Full HTTP URL or a local resource URI for chain snapshots that do not come from HTTP. */
    val normalizedUrl: String,
    /** Owner captured by the caller. Null means public data; deleting a wallet deletes its owned rows. */
    val metaAccountId: Long?,
    /** JSON validated by the consuming repository. */
    val payload: String,
    /** Request start (HTTP) or snapshot write time, in Unix milliseconds. */
    val fetchedAt: Long,
    /** Freshness deadline; 0 for stale local snapshots, Long.MAX_VALUE for durable cache metadata. */
    val expiresAt: Long,
    /** Includes the owner so identical URLs in different wallets cannot overwrite one another. */
    @PrimaryKey val key: String = ResponseCacheKey(normalizedUrl, metaAccountId).storageKey
)
