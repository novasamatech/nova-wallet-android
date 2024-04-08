package io.novafoundation.nova.core_db.model.chain.account

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.novafoundation.nova.core.model.CryptoType
import java.util.UUID

@Entity(
    tableName = MetaAccountLocal.TABLE_NAME,
    indices = [
        Index(value = ["substrateAccountId"]),
        Index(value = ["ethereumAddress"])
    ]
)
class MetaAccountLocal(
    val substratePublicKey: ByteArray?,
    val substrateCryptoType: CryptoType?,
    val substrateAccountId: ByteArray?,
    val ethereumPublicKey: ByteArray?,
    val ethereumAddress: ByteArray?,
    val name: String,
    val parentMetaId: Long?,
    val isSelected: Boolean,
    val position: Int,
    val type: Type,
    @ColumnInfo(defaultValue = "ACTIVE")
    val status: Status,
    val globallyUniqueId: String
) {

    enum class Status {
        ACTIVE, DEACTIVATED
    }

    companion object Table {
        const val TABLE_NAME = "meta_accounts"

        object Column {
            const val SUBSTRATE_PUBKEY = "substratePublicKey"
            const val SUBSTRATE_CRYPTO_TYPE = "substrateCryptoType"
            const val SUBSTRATE_ACCOUNT_ID = "substrateAccountId"

            const val ETHEREUM_PUBKEY = "ethereumPublicKey"
            const val ETHEREUM_ADDRESS = "ethereumAddress"

            const val NAME = "name"
            const val IS_SELECTED = "isSelected"
            const val POSITION = "position"
            const val ID = "id"
        }

        fun generateGloballyUniqueId(): String {
            return UUID.randomUUID().toString()
        }
    }

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    enum class Type {
        SECRETS, WATCH_ONLY, PARITY_SIGNER, LEDGER, POLKADOT_VAULT, PROXIED
    }
}

class MetaAccountPositionUpdate(
    val id: Long,
    val position: Int
)
