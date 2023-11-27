package io.novafoundation.nova.core_db.model.chain.account

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.novafoundation.nova.core.model.CryptoType

@Entity(
    tableName = "proxy_accounts",
    foreignKeys = [
        ForeignKey(
            parentColumns = ["id"],
            childColumns = ["metaId"],
            entity = MetaAccountLocal::class,
            onDelete = ForeignKey.CASCADE
        ),
    ],
    primaryKeys = ["metaId", "delegatorAccountId", "chainId", "rightType"]
)
class ProxyAccountLocal(
    val metaId: Long,
    val chainId: String,
    val delegatorAccountId: ByteArray,
    val rightType: RightType,
    val status: Status,
) {

    enum class RightType {
        ANY, UNKNOWN //TODO add more
    }

    enum class Status {
        ACTIVE, DEACTIVATED
    }
}
