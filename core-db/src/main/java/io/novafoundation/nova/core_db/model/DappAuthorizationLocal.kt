package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dapp_authorizations")
class DappAuthorizationLocal(
    @PrimaryKey
    val baseUrl: String,
    val authorized: Boolean?
)
