package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal

/**
 * Whether one wallet shows one asset.
 *
 * A row is a decision that has been made - by the user in Manage Tokens, by the app noticing a
 * balance, or by the user moving their own funds into the token. No row means nobody has decided
 * yet, and the curated default list answers instead.
 *
 * Display only: syncing does not consult this table, every asset of a connected chain is subscribed
 * to regardless. That is what lets a balance arriving in a token nobody has decided about be
 * noticed at all.
 */
@Entity(
    tableName = "chain_asset_visibility",
    primaryKeys = ["metaId", "chainId", "assetId"],
    foreignKeys = [
        ForeignKey(
            entity = MetaAccountLocal::class,
            parentColumns = ["id"],
            childColumns = ["metaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChainAssetLocal::class,
            parentColumns = ["id", "chainId"],
            childColumns = ["assetId", "chainId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("metaId"), Index("assetId", "chainId")]
)
class ChainAssetVisibilityLocal(
    val metaId: Long,
    val chainId: String,
    val assetId: Int,
    val visible: Boolean
)
