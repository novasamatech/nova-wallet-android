package io.novafoundation.nova.core_db.model.operation

import androidx.room.Embedded
import io.novafoundation.nova.core_db.model.AssetAndChainId

sealed interface OperationTypeLocal {

    val foreignKey: OperationForeignKey

    class OperationForeignKey(
        val operationId: String,
        val address: String,
        @Embedded
        val assetId: AssetAndChainId
    )
}
