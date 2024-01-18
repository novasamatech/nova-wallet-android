package io.novafoundation.nova.feature_account_api.domain.model

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

class MetaAccountAssetBalance(
    val metaId: Long,
    val freeInPlanks: BigInteger,
    val reservedInPlanks: BigInteger,
    val offChainBalance: BigInteger?,
    val precision: Int,
    val rate: BigDecimal?
)

class MetaAccountWithTotalBalance(
    val metaAccount: MetaAccount,
    val proxyMetaAccount: MetaAccount?,
    val proxyChain: Chain?,
    val totalBalance: BigDecimal,
    val currency: Currency,
    val hasUpdates: Boolean,
)
