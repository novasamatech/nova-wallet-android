package io.novafoundation.nova.feature_account_api.domain.model

import java.math.BigDecimal
import java.math.BigInteger

class MetaAccountWithAssetBalance(
    val metaId: Long,
    val name: String,
    val isSelected: Boolean,
    val type: LightMetaAccount.Type,
    val substrateAccountId: ByteArray,
    val freeInPlanks: BigInteger,
    val reservedInPlanks: BigInteger,
    val precision: Int,
    val priceRate: BigDecimal?,
    val currencySymbol: String?,
    val currencyCode: String
)

class MetaAccountWithTotalBalance(
    val metaId: Long,
    val name: String,
    val type: LightMetaAccount.Type,
    val isSelected: Boolean,
    val substrateAccountId: ByteArray,
    val totalBalance: BigDecimal,
    val currencySymbol: String?,
    val currencyCode: String
)
