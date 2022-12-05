package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import io.novafoundation.nova.common.utils.Identifiable

@Entity(tableName = "governance_dapps", primaryKeys = ["chainId", "name"])
data class GovernanceDAppLocal(
    val chainId: String,
    val name: String,
    val referendumUrlV1: String?,
    val referendumUrlV2: String?,
    val iconUrl: String,
    val details: String,
) : Identifiable {

    override val identifier: String
        get() = "$chainId|$name"
}
