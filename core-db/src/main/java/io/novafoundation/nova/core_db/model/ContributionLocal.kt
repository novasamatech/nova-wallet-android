package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import io.novafoundation.nova.common.utils.Identifiable
import java.math.BigInteger

@Entity(tableName = "contributions", primaryKeys = ["metaId", "chainId", "assetId", "paraId", "sourceId"])
class ContributionLocal(
    val metaId: Long,
    val chainId: String,
    val assetId: Int,
    val paraId: BigInteger,
    val amountInPlanks: BigInteger,
    val sourceId: String,
    val unlockBlock: BigInteger,
    val leaseDepositor: ByteArray // AccountId
) : Identifiable {
    override val identifier: String
        get() = "$metaId|$chainId|$paraId|$sourceId"
}
