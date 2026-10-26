package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRankedValidatorsRemote
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorsRemote
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

internal fun mapBittensorValidatorPreview(chain: Chain, row: BittensorRankedValidatorsRemote.Item): BittensorValidator? {
    if (row.eligible != true || row.recommendationClass !in setOf("stable", "balanced", "higherUpside")) return null
    val address = row.hotkey ?: return null
    val hotkey = runCatching { AccountIdKey(chain.accountIdOf(address)) }.getOrNull() ?: return null
    return BittensorValidator(hotkey, row.validatorName?.trim()?.takeIf { it.isNotBlank() } ?: address)
}

internal fun mapBittensorDirectoryPreview(chain: Chain, row: BittensorValidatorsRemote.Item): BittensorValidator? {
    val address = row.hotkey ?: return null
    val hotkey = runCatching { AccountIdKey(chain.accountIdOf(address)) }.getOrNull() ?: return null
    val name = row.identitySourceName?.trim()?.takeIf { it.isNotBlank() }
        ?: row.stakeSourceName?.trim()?.takeIf { it.isNotBlank() }
        ?: address
    return BittensorValidator(hotkey, name)
}
