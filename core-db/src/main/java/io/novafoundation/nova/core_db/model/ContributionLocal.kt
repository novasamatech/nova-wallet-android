package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import java.math.BigInteger

@Entity(tableName = "contributions", primaryKeys = ["metaId", "chainId", "paraId"])
data class ContributionLocal(
    val metaId: Long,
    val chainId: String,
    val paraId: BigInteger,
    val amountInPlanks: BigInteger,
    val sourceName: String?,
    val returnsIn: Long,
    val type: Type,
) {

    enum class Type {
        DIRECT, ACALA, PARALLEL
    }
}
