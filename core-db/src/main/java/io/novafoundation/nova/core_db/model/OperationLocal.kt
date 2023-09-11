package io.novafoundation.nova.core_db.model

import androidx.room.Embedded
import androidx.room.Entity
import java.math.BigInteger

@Entity(
    tableName = "operations",
    primaryKeys = ["id", "address", "chainId", "chainAssetId"]
)
data class OperationLocal(
    val id: String,
    val address: String,
    val chainId: String,
    val chainAssetId: Int,
    val time: Long,
    val status: Status,
    val source: Source,
    val operationType: Type,
    @Embedded(prefix = "extrinsicContent_")
    val extrinsicContent: ExtrinsicContent? = null,
    val amount: BigInteger? = null,
    val sender: String? = null,
    val receiver: String? = null,
    val hash: String? = null,
    val fee: BigInteger? = null,
    val isReward: Boolean? = null,
    val era: Int? = null,
    val validator: String? = null,
    val poolId: Int? = null,
) {
    enum class Type {
        EXTRINSIC, TRANSFER, REWARD, POOL_REWARD
    }

    enum class Source {
        BLOCKCHAIN, REMOTE, APP
    }

    enum class Status {
        PENDING, COMPLETED, FAILED
    }

    enum class ExtrinsicContentType {
        SUBSTRATE_CALL, SMART_CONTRACT_CALL
    }

    class ExtrinsicContent(
        val type: ExtrinsicContentType,
        val module: String? = null,
        val call: String? = null,
    )

    companion object {

        fun manualTransfer(
            hash: String,
            address: String,
            chainId: String,
            chainAssetId: Int,
            amount: BigInteger,
            senderAddress: String,
            receiverAddress: String,
            fee: BigInteger?,
            status: Status,
            source: Source
        ) = OperationLocal(
            id = hash,
            hash = hash,
            address = address,
            chainId = chainId,
            chainAssetId = chainAssetId,
            time = System.currentTimeMillis(),
            amount = amount,
            sender = senderAddress,
            receiver = receiverAddress,
            fee = fee,
            status = status,
            source = source,
            operationType = Type.TRANSFER
        )
    }
}
