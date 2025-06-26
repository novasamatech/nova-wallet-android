package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal

@Entity(
    tableName = "multisig_operation_call",
    primaryKeys = ["metaId", "chainId", "callHash"],
    foreignKeys = [
        ForeignKey(
            entity = MetaAccountLocal::class,
            parentColumns = ["id"],
            childColumns = ["metaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChainLocal::class,
            parentColumns = ["id"],
            childColumns = ["chainId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MultisigOperationCallLocal(
    val metaId: Long,
    val chainId: String,
    val callHash: String,
    val callInstance: String
)
