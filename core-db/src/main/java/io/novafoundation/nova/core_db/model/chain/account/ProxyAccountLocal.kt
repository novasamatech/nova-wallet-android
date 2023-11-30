package io.novafoundation.nova.core_db.model.chain.account

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import io.novafoundation.nova.common.utils.Identifiable
import jp.co.soramitsu.fearless_utils.extensions.toHexString

@Entity(
    tableName = "proxy_accounts",
    foreignKeys = [
        ForeignKey(
            parentColumns = ["id"],
            childColumns = ["metaId"],
            entity = MetaAccountLocal::class,
            onDelete = ForeignKey.CASCADE
        ),
    ],
    primaryKeys = ["metaId", "proxiedAccountId", "chainId", "proxyType"]
)
data class ProxyAccountLocal(
    val metaId: Long,
    val chainId: String,
    val proxiedAccountId: ByteArray,
    val proxyType: ProxyType,
    val status: Status,
) : Identifiable {

    enum class ProxyType {
        ANY, UNKNOWN // TODO add more
    }

    enum class Status {
        ACTIVE, DEACTIVATED
    }

    @Ignore
    override val identifier: String = "$metaId:$chainId:${proxiedAccountId.toHexString()}:$proxyType"
}
