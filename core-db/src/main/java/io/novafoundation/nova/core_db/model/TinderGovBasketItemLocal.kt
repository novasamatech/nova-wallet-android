package io.novafoundation.nova.core_db.model

import androidx.room.Entity
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
    val conviction: Conviction,
    val voteType: VoteType
) {

    enum class Conviction {
        None,
        LOCKED_1x,
        LOCKED_2x,
        LOCKED_3x,
        LOCKED_4x,
        LOCKED_5x,
        LOCKED_6x
    }

    enum class VoteType {
        AYE, NAY, ABSTAIN
    }
}
