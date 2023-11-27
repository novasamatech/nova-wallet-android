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
)
