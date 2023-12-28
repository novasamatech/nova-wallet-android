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
            childColumns = ["proxiedMetaId"],
            entity = MetaAccountLocal::class,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            parentColumns = ["id"],
            childColumns = ["proxyMetaId"],
            entity = MetaAccountLocal::class,
            onDelete = ForeignKey.CASCADE
        ),
    ],
    primaryKeys = ["proxyMetaId", "proxiedAccountId", "chainId", "proxyType"]
)
data class ProxyAccountLocal(
    val proxiedMetaId: Long,
    val proxyMetaId: Long,
    val chainId: String,
    val proxiedAccountId: ByteArray,
    val proxyType: String
) : Identifiable {

    @Ignore
    override val identifier: String = makeIdentifier(proxyMetaId, chainId, proxiedAccountId, proxyType)

    companion object {
        fun makeIdentifier(
            proxyMetaId: Long,
            chainId: String,
            proxiedAccountId: ByteArray,
            proxyType: String
        ): String {
            return "$proxyMetaId:$chainId:${proxiedAccountId.toHexString()}:$proxyType"
        }
    }
}
