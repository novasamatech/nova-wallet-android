package io.novafoundation.nova.core_db.model.chain

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import io.novafoundation.nova.common.utils.Identifiable

@Entity(tableName = "chains")
data class ChainLocal(
    @PrimaryKey val id: String,
    val parentId: String?,
    val name: String,
    val icon: String,
    @Embedded
    val types: TypesConfig?,
    @Embedded
    val externalApi: ExternalApi?,
    val prefix: Int,
    val isEthereumBased: Boolean,
    val isTestNet: Boolean,
    val hasCrowdloans: Boolean,
    val governance: String,
    val additional: String?,
) : Identifiable {

    @Ignore
    override val identifier: String = id

    data class TypesConfig(
        val url: String,
        val overridesCommon: Boolean,
    )

    data class ExternalApi(
        @Embedded(prefix = "staking_")
        val staking: Section?,

        @Embedded(prefix = "history_")
        val history: Section?,

        @Embedded(prefix = "crowdloans_")
        val crowdloans: Section?,

        @Embedded(prefix = "governance_")
        val governance: Section?,
    ) {

        data class Section(val url: String, val type: String)
    }
}
