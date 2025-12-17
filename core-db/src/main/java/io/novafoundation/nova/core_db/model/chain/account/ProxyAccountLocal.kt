package io.novafoundation.nova.core_db.model.chain.account

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novasama.substrate_sdk_android.extensions.toHexString

@Entity(
    tableName = "proxy_accounts",
    foreignKeys = [
        ForeignKey(
            parentColumns = ["id"],
            childColumns = ["proxiedMetaId"],
            entity = MetaAccountLocal::class,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            parentColumns = ["id"],
            childColumns = ["proxyMetaId"],
            entity = MetaAccountLocal::class,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            parentColumns = ["id"],
            childColumns = ["chainId"],
            entity = ChainLocal::class,
            onDelete = ForeignKey.CASCADE
        ),
    ],
    primaryKeys = ["proxyMetaId", "proxiedAccountId", "chainId", "proxyType"]
)
data class ProxyAccountLocal(
    val proxiedMetaId: Long,
    val proxyMetaId: Long,
    val chainId: String,
    @Deprecated("Unused")
    val proxiedAccountId: ByteArray,
    val proxyType: String
) : Identifiable {

    @Ignore
    override val identifier: String = makeIdentifier(proxyMetaId, chainId, proxiedAccountId, proxyType)

    companion object {
        fun makeIdentifier(
            proxyMetaId: Long,
            chainId: String,
            proxiedAccountId: ByteArray,
            proxyType: String
        ): String {
            return "$proxyMetaId:$chainId:${proxiedAccountId.toHexString()}:$proxyType"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProxyAccountLocal) return false

        if (proxiedMetaId != other.proxiedMetaId) return false
        if (proxyMetaId != other.proxyMetaId) return false
        if (chainId != other.chainId) return false
        if (!proxiedAccountId.contentEquals(other.proxiedAccountId)) return false
        if (proxyType != other.proxyType) return false
        if (identifier != other.identifier) return false

        return true
    }

    override fun hashCode(): Int {
        var result = proxiedMetaId.hashCode()
        result = 31 * result + proxyMetaId.hashCode()
        result = 31 * result + chainId.hashCode()
        result = 31 * result + proxiedAccountId.contentHashCode()
        result = 31 * result + proxyType.hashCode()
        result = 31 * result + identifier.hashCode()
        return result
    }
}
