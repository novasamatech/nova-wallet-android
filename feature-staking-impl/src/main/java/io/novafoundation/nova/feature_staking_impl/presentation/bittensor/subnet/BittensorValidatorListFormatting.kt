package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.common.utils.TokenSymbol
import java.math.BigDecimal
import java.text.Normalizer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorReadFailureKind
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorReadFailureKind

data class BittensorValidatorListHeader(val count: String, val sorting: String)

/** [total] is the directory size of the same snapshot the rows come from; null while the directory loads. */
data class BittensorValidatorRows(val rows: List<BittensorValidatorUi>, val total: Int?)

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

data class BittensorValidatorListFailure(val title: String, val message: String)

/** Inline warning for a validator list that did not load. A device that cannot attest gets its own explanation, as on iOS. */
internal fun bittensorValidatorListFailure(cause: Throwable, resources: ResourceManager): BittensorValidatorListFailure {
    val message = when (cause.bittensorReadFailureKind()) {
        BittensorReadFailureKind.DEVICE_UNSUPPORTED -> R.string.bittensor_validators_fetch_failed_device_message
        BittensorReadFailureKind.GENERIC, BittensorReadFailureKind.SLOW_NETWORK -> R.string.bittensor_validators_fetch_failed_message
    }
    return BittensorValidatorListFailure(
        title = resources.getString(R.string.bittensor_validators_fetch_failed_title),
        message = resources.getString(message)
    )
}
