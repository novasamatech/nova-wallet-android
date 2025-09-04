package io.novafoundation.nova.core_db.model.chain.account

import androidx.room.Embedded
import androidx.room.Relation

interface JoinedMetaAccountInfo {

    val metaAccount: MetaAccountLocal

    val chainAccounts: List<ChainAccountLocal>

    val proxyAccountLocal: ProxyAccountLocal?
}

class RelationJoinedMetaAccountInfo(
    @Embedded
    override val metaAccount: MetaAccountLocal,

    @Relation(parentColumn = "id", entityColumn = "metaId", entity = ChainAccountLocal::class)
    override val chainAccounts: List<ChainAccountLocal>,

    @Relation(parentColumn = "id", entityColumn = "proxiedMetaId", entity = ProxyAccountLocal::class)
    override val proxyAccountLocal: ProxyAccountLocal?,
) : JoinedMetaAccountInfo
