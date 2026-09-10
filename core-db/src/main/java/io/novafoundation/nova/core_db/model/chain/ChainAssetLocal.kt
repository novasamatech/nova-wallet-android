package io.novafoundation.nova.core_db.model.chain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import io.novafoundation.nova.common.utils.Identifiable

@Entity(
    tableName = "chain_assets",
    primaryKeys = ["chainId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = ChainLocal::class,
            parentColumns = ["id"],
            childColumns = ["chainId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chainId"])
    ]
)
data class ChainAssetLocal(
    val id: Int,
    val chainId: String,
    val name: String,
    val symbol: String,
    val priceId: String?,
    val staking: String,
    val precision: Int,
    val icon: String?,
    val type: String?,
    @ColumnInfo(defaultValue = SOURCE_DEFAULT)
    val source: AssetSourceLocal,
    val buyProviders: String?,
    val sellProviders: String?,
    val typeExtras: String?,
    /**
     * TODO unused - drop the column.
     *
     * Show/hide moved to `chain_asset_visibility`, which is per wallet, and syncing no longer
     * consults anything here. Nothing reads or carries this value any more; it is always written as
     * [ENABLED_DEFAULT_BOOL].
     *
     * Removing it needs `chain_assets` recreated: SQLite gained ALTER TABLE DROP COLUMN in 3.35, but
     * minSdk 24 runs system SQLite as old as 3.9. That recreation has to move seven child tables'
     * foreign keys with it, so it belongs in its own migration verified on a device - and until it
     * lands the field must stay declared, or Room's schema check fails at open.
     */
    @ColumnInfo(defaultValue = ENABLED_DEFAULT_STR)
    val enabled: Boolean,
) : Identifiable {

    companion object {

        const val SOURCE_DEFAULT = "DEFAULT"
        const val ENABLED_DEFAULT_STR = "1"
        const val ENABLED_DEFAULT_BOOL = true
    }

    @Ignore
    override val identifier: String = "$id:$chainId"
}
