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

    val issuanceType: IssuanceType,
    val issuanceTotal: BigInteger? = null,
    val issuanceMyEdition: String? = null,
    val issuanceMyAmount: BigInteger? = null,

    val price: BigInteger? = null,
    // use null to indicate non-fungible price
    val pricedUnits: BigInteger? = null
) : Identifiable {

    enum class Type {
        UNIQUES, RMRK1, RMRK2, PDC20, KODADOT, UNIQUE_NETWORK
    }

    enum class IssuanceType {

        // issuanceMyEdition: optional
        UNLIMITED,

        // issuanceMyEdition + issuanceTotal
        LIMITED,

        // issuanceTotal + issuanceFungible
        FUNGIBLE
    }

    override fun equals(other: Any?): Boolean {
        return other is NftLocal &&
            identifier == other.identifier &&
            // metadata is either direct data or a link to immutable distributed storage
            metadata.optionalContentEquals(other.metadata) &&
            price == other.price
    }
}
