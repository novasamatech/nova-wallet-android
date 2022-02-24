package io.novafoundation.nova.core_db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.utils.optionalContentEquals
import java.math.BigInteger

@Entity(tableName = "nfts")
class NftLocal(
    @PrimaryKey
    override val identifier: String,
    @ColumnInfo(index = true)
    val metaId: Long,
    val chainId: String,
    val collectionId: String?,
    val instanceId: String?,
    val metadata: ByteArray?,
    // --- metadata fields ---
    // name is always be present. null in case it is not loaded (nft is partially loaded)
    val name: String? = null,
    val label: String? = null,
    val media: String? = null,
    val price: BigInteger? = null,
    // --- metadata fields ---

    val type: Type
): Identifiable {

    enum class Type {
        UNIQUES, RMRK1
    }

    override fun equals(other: Any?): Boolean {
        return other is NftLocal &&
            identifier == other.identifier &&
            // metadata is either direct data or a link to immutable distributed storage
            metadata.optionalContentEquals(other.metadata)
            && price == other.price
    }
}
