package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import io.novafoundation.nova.core_db.model.common.ConvictionLocal
import java.math.BigInteger

@Entity(
    tableName = "tinder_gov_voting_power",
    primaryKeys = ["chainId"]
)
data class TinderGovVotingPowerLocal(
    val chainId: String,
    val amount: BigInteger,
    val conviction: ConvictionLocal
)
