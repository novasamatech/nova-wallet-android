package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourite_dapps")
class FavouriteDAppLocal(
    @PrimaryKey
    val url: String,
    val label: String,
    val icon: String?
)


