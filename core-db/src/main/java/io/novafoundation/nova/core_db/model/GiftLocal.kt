package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import java.math.BigInteger

@Entity(
    tableName = "gifts",
    foreignKeys = [
        ForeignKey(
            entity = ChainAssetLocal::class,
            parentColumns = ["id", "chainId"],
            childColumns = ["assetId", "chainId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class GiftLocal(
    val amount: BigInteger,
    val giftAccountId: ByteArray,
    val chainId: String,
    val assetId: Int,
    val status: Status
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    enum class Status {
        PENDING,
        CLAIMED,
        RECLAIMED
    }
}
