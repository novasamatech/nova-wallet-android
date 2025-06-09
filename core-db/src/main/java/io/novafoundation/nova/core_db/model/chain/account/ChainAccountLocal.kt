package io.novafoundation.nova.core_db.model.chain.account

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.novafoundation.nova.core.model.CryptoType

@Entity(
    tableName = "chain_accounts",
    foreignKeys = [
        // no foreign key for `chainId` since we do not want ChainAccounts to be deleted or modified when chain is deleted
        // but rather keep it in db in case future UI will show them somehow

        ForeignKey(
            parentColumns = ["id"],
            childColumns = ["metaId"],
            entity = MetaAccountLocal::class,
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = ["chainId"]),
        Index(value = ["metaId"]),
        Index(value = ["accountId"]),
    ],
    primaryKeys = ["metaId", "chainId"]
)
class ChainAccountLocal(
    val metaId: Long,
    val chainId: String,
    val publicKey: ByteArray?,
    val accountId: ByteArray,
    val cryptoType: CryptoType?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChainAccountLocal) return false

        if (metaId != other.metaId) return false
        if (chainId != other.chainId) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!accountId.contentEquals(other.accountId)) return false
        if (cryptoType != other.cryptoType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = metaId.hashCode()
        result = 31 * result + chainId.hashCode()
        result = 31 * result + (publicKey?.contentHashCode() ?: 0)
        result = 31 * result + accountId.contentHashCode()
        result = 31 * result + (cryptoType?.hashCode() ?: 0)
        return result
    }
}

