package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.common.utils.TokenSymbol
import java.math.BigDecimal
import java.text.Normalizer

data class BittensorValidatorListHeader(val count: String, val sorting: String)

/**
 * [total] is the directory size of the same snapshot the rows come from; null while the directory loads.
 * [partial]: the backend marks the directory PARTIAL, so its size is no total and the header shows none.
 */
data class BittensorValidatorRows(val rows: List<BittensorValidatorUi>, val total: Int?, val partial: Boolean = false)

data class BittensorValidatorStakeDisplay(val symbol: TokenSymbol, val nativePrice: BigDecimal?)

internal fun bittensorValidatorDisplayName(validator: BittensorSubnetValidator): String {
    val name = validator.validator.name.trim()
    return name.takeIf { it.isNotEmpty() && it != validator.address } ?: validator.address.ellipsizeAddress()
}

internal fun matchesBittensorValidator(validator: BittensorSubnetValidator, query: String): Boolean {
    val text = normalizedBittensorSearch(query.trim())
    return text.isEmpty() || normalizedBittensorSearch(validator.validator.name).contains(text, ignoreCase = true) ||
        validator.address.contains(text, ignoreCase = true)
}

private fun normalizedBittensorSearch(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
