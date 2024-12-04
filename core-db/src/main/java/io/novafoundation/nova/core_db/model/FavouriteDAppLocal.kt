package io.novafoundation.nova.core_db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.novafoundation.nova.common.utils.Identifiable

@Entity(tableName = "favourite_dapps")
class FavouriteDAppLocal(
    @PrimaryKey
    val url: String,
    val label: String,
    val icon: String?,
    @ColumnInfo(defaultValue = "0")
    val orderingIndex: Int
) : Identifiable {

    override val identifier: String = url
}
