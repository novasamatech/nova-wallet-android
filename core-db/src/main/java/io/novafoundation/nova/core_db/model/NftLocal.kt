package io.novafoundation.nova.core_db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.utils.optionalContentEquals
import java.math.BigInteger

@Entity(tableName = "nfts")
data class NftLocal(
    @PrimaryKey
    override val identifier: String,
    @ColumnInfo(index = true)
    val metaId: Long,
    val chainId: String,
    val collectionId: String,
    val instanceId: String?,
    val metadata: ByteArray?,
    val type: Type,

    val wholeDetailsLoaded: Boolean,

    // --- metadata fields ---
    val name: String? = null,
    val label: String? = null,
    val media: String? = null,
    // --- !metadata fields ---

    val issuanceTotal: Int? = null,
    val issuanceMyEdition: String? = null,

    val price: BigInteger? = null,
) : Identifiable {

    enum class Type {
        UNIQUES, RMRK1, RMRK2
    }

    override fun equals(other: Any?): Boolean {
        return other is NftLocal &&
            identifier == other.identifier &&
            // metadata is either direct data or a link to immutable distributed storage
            metadata.optionalContentEquals(other.metadata) &&
            price == other.price
    }
}
