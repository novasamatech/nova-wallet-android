package io.novafoundation.nova.core_db.model.operation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import io.novafoundation.nova.core_db.model.AssetAndChainId

@Entity(
    tableName = "operations",
    primaryKeys = ["id", "address", "chainId", "assetId"],
)
data class OperationBaseLocal(
    val id: String,
    val address: String,
    @Embedded
    val assetId: AssetAndChainId,
    val time: Long,
    val status: Status,
    val source: Source,
    @ColumnInfo(index = true)
    val hash: String?,
) {

    enum class Source {
        BLOCKCHAIN, REMOTE, APP
    }

    enum class Status {
        PENDING, COMPLETED, FAILED
    }
}
