package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import io.novafoundation.nova.core_db.model.common.ConvictionLocal
import java.math.BigInteger

@Entity(
    tableName = "tinder_gov_basket",
    primaryKeys = ["referendumId", "metaId", "chainId"]
)
data class TinderGovBasketItemLocal(
    val referendumId: BigInteger,
    val metaId: Long,
    val chainId: String,
    val amount: BigInteger,
    val conviction: ConvictionLocal,
    val voteType: VoteType
) {

    enum class VoteType {
        AYE, NAY, ABSTAIN
    }
}
