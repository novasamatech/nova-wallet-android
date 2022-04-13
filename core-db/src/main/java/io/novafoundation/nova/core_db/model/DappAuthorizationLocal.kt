package io.novafoundation.nova.core_db.model

import androidx.room.Entity

@Entity(
    tableName = "dapp_authorizations",
    primaryKeys = ["baseUrl", "metaId"]
)
class DappAuthorizationLocal(
    val baseUrl: String,
    val metaId: Long,
    val dAppTitle: String?,
    val authorized: Boolean?
)
