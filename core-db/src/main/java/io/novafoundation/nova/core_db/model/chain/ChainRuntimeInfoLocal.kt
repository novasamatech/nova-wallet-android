package io.novafoundation.nova.core_db.model.chain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "chain_runtimes",
    primaryKeys = ["chainId"],
    indices = [
        Index(value = ["chainId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ChainLocal::class,
            parentColumns = ["id"],
            childColumns = ["chainId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class ChainRuntimeInfoLocal(
    val chainId: String,
    val syncedVersion: Int,
    val remoteVersion: Int,
    val transactionVersion: Int?,
    @ColumnInfo(defaultValue = DEFAULT_MIGRATOR_VERSION)
    val localMigratorVersion: Int
) {

    companion object {
        const val DEFAULT_MIGRATOR_VERSION = "1"
    }
}
