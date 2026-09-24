package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal

/**
 * Settings that belong to one wallet rather than to the install.
 *
 * A row appears only once a wallet is set to something other than the default, so a wallet with no
 * row is simply a wallet nobody has changed anything on.
 */
@Entity(
    tableName = "meta_account_settings",
    foreignKeys = [
        ForeignKey(
            entity = MetaAccountLocal::class,
            parentColumns = ["id"],
            childColumns = ["metaId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class MetaAccountSettingsLocal(
    @PrimaryKey val metaId: Long,
    val autoAddTokensWithBalance: Boolean
)
