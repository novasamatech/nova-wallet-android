package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import io.novafoundation.nova.core_db.model.chain.ChainLocal

@Entity(
    tableName = "multisig_operation_call",
    foreignKeys = [
        ForeignKey(
            entity = ChainLocal::class,
            parentColumns = ["id"],
            childColumns = ["chainId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MultisigOperationCallLocal(
    @PrimaryKey val operationId: String,
    val chainId: String,
    val callHash: String,
    val callInstance: String
)
