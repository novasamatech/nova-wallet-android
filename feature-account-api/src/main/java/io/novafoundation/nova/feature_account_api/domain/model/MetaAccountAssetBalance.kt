package io.novafoundation.nova.feature_account_api.domain.model

import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

class MetaAccountAssetBalance(
    val metaId: Long,
    val freeInPlanks: BigInteger,
    val reservedInPlanks: BigInteger,
    val offChainBalance: BigInteger?,
    val precision: Precision,
    val rate: BigDecimal?
)

sealed interface MetaAccountListingItem {

    val metaAccount: MetaAccount

    val hasUpdates: Boolean

    val totalBalance: BigDecimal

    val currency: Currency

    class Proxied(
        val proxyMetaAccount: MetaAccount,
        val proxyChain: Chain,
        override val totalBalance: BigDecimal,
        override val currency: Currency,
        override val metaAccount: ProxiedMetaAccount,
        override val hasUpdates: Boolean
    ) : MetaAccountListingItem

    class Multisig(
        val signatory: MetaAccount,
        override val totalBalance: BigDecimal,
        override val currency: Currency,
        override val metaAccount: MultisigMetaAccount,
        override val hasUpdates: Boolean
    ) : MetaAccountListingItem

    class TotalBalance(
        override val totalBalance: BigDecimal,
        override val currency: Currency,
        override val metaAccount: MetaAccount,
        override val hasUpdates: Boolean
    ) : MetaAccountListingItem
}
