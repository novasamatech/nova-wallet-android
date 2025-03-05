package io.novafoundation.nova.core_db.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal

@Entity(
    tableName = "browser_tabs",
    foreignKeys = [
        ForeignKey(
            entity = MetaAccountLocal::class,
            parentColumns = ["id"],
            childColumns = ["metaId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BrowserTabLocal(
    @PrimaryKey(autoGenerate = false) val id: String,
    val metaId: Long,
    val currentUrl: String,
    val creationTime: Long,
    val pageName: String?,
    val pageIconPath: String?,
    @Embedded(prefix = "dappMetadata_")
    val knownDAppMetadata: KnownDAppMetadata?,
    val pagePicturePath: String?
) {

    class KnownDAppMetadata(val iconLink: String)
}
