package io.novafoundation.nova.core_db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import io.novafoundation.nova.core_db.model.chain.MetaAccountLocal

@Entity(
    tableName = "wallet_connect_pairings",
    foreignKeys = [
        ForeignKey(
            parentColumns = ["id"],
            childColumns = ["metaId"],
            entity = MetaAccountLocal::class,
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
class WalletConnectPairingLocal(
    @PrimaryKey
    val pairingTopic: String,
    @ColumnInfo(index = true)
    val metaId: Long
)
