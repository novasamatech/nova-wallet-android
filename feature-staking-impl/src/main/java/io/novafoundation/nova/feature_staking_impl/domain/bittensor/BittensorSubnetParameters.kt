package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.common.utils.isZero
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

data class BittensorSubnetEmissionRow(
    val netuid: BittensorNetuid,
    val registrationBlock: BigInteger?,
    val taoInEmission: BigInteger?
)

data class BittensorSubnetEmissionSnapshot(
    val rows: List<BittensorSubnetEmissionRow?>,
    val enabled: Boolean?
)

data class BittensorSubnetEmission(
    val reference: BittensorSubnetReference,
    val fraction: BigDecimal?
)

internal fun bittensorSubnetEmissionShare(
    reference: BittensorSubnetReference,
    snapshot: BittensorSubnetEmissionSnapshot
): BigDecimal? {
    val rows = snapshot.rows
    val selected = rows.filterNotNull().singleOrNull { it.netuid == reference.subnetId } ?: return null
    if (selected.registrationBlock != reference.registrationBlock) return null
    val enabled = snapshot.enabled ?: return null
    if (!enabled) return BigDecimal.ZERO

    if (rows.any { it == null } || rows.map { it?.netuid }.distinct().size != rows.size) return null
    val emissions = rows.map { row -> row?.taoInEmission?.takeIf { it.isNonNegative } ?: return null }
    val total = emissions.fold(BigInteger.ZERO, BigInteger::add)
    if (total.isZero) return null
    return requireNotNull(selected.taoInEmission).toBigDecimal().divide(total.toBigDecimal(), MathContext.DECIMAL128)
}

internal fun bittensorPoolNativeShare(nativeReserve: BigInteger?, alphaReserve: BigInteger?): BigDecimal? {
    if (nativeReserve == null || alphaReserve == null || !nativeReserve.isNonNegative || !alphaReserve.isNonNegative) return null
    val total = nativeReserve + alphaReserve
    if (total.isZero) return null
    return nativeReserve.toBigDecimal().divide(total.toBigDecimal(), MathContext.DECIMAL128)
}

internal fun bittensorAlphaPoolValueInRao(alphaReserve: BigInteger?, priceInRao: BigInteger?): BigInteger? {
    if (alphaReserve == null || priceInRao == null || !alphaReserve.isNonNegative || !priceInRao.isNonNegative) return null
    return alphaReserve * priceInRao / BigInteger.TEN.pow(BITTENSOR_ALPHA_DECIMALS)
}
