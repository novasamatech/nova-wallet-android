package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.common.ConvictionLocal
import java.math.BigInteger

@Entity(
    tableName = "tinder_gov_voting_power",
    primaryKeys = ["metaId", "chainId"],
    foreignKeys = [
        ForeignKey(
            parentColumns = ["id"],
            childColumns = ["metaId"],
            entity = MetaAccountLocal::class,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            parentColumns = ["id"],
            childColumns = ["chainId"],
            entity = ChainLocal::class,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TinderGovVotingPowerLocal(
    val metaId: Long,
    val chainId: String,
    val amount: BigInteger,
    val conviction: ConvictionLocal
)
