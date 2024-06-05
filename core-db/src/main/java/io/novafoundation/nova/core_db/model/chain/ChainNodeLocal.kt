package io.novafoundation.nova.core_db.model.chain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import io.novafoundation.nova.common.utils.Identifiable

@Entity(
    tableName = "chain_nodes",
    primaryKeys = ["chainId", "url"],
    foreignKeys = [
        ForeignKey(
            entity = ChainLocal::class,
            parentColumns = ["id"],
            childColumns = ["chainId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chainId"])
    ]
)
data class ChainNodeLocal(
    val chainId: String,
    val url: String,
    val name: String,
    @ColumnInfo(defaultValue = "0")
    val orderId: Int,
    @ColumnInfo(defaultValue = DEFAULT_IS_CUSTOM_NODE_STR)
    val isCustom: Boolean
) : Identifiable {

    companion object {

        const val DEFAULT_IS_CUSTOM_NODE_STR = "1"
    }

    @Ignore
    override val identifier: String = "$chainId:$url"
}
