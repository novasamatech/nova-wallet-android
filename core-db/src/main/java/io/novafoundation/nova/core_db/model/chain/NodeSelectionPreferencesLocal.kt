package io.novafoundation.nova.core_db.model.chain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import io.novafoundation.nova.common.utils.Identifiable

@Entity(
    tableName = "node_selection_preferences",
    primaryKeys = ["chainId"],
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
data class NodeSelectionPreferencesLocal(
    val chainId: String,
    @ColumnInfo(defaultValue = DEFAULT_AUTO_BALANCE_DEFAULT_STR)
    val autoBalanceEnabled: Boolean,
    val selectedNodeUrl: String?
) : Identifiable {

    companion object {

        const val DEFAULT_AUTO_BALANCE_DEFAULT_STR = "1"
        const val DEFAULT_AUTO_BALANCE_BOOLEAN = true
    }

    @Ignore
    override val identifier: String = chainId
}
