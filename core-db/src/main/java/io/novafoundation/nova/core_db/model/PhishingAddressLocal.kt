package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phishing_addresses")
data class PhishingAddressLocal(
    @PrimaryKey val publicKey: String
)
