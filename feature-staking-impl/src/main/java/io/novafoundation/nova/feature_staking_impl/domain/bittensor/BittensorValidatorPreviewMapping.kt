package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRankedValidatorsRemote
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorsRemote
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CancellationException

internal fun mapBittensorValidatorPreview(chain: Chain, row: BittensorRankedValidatorsRemote.Item): BittensorValidator? {
    if (row.eligible != true || row.recommendationClass !in setOf("stable", "balanced", "higherUpside")) return null
    val address = row.hotkey ?: return null
    val hotkey = mapBittensorPreviewHotkey(chain, address) ?: return null
    return BittensorValidator(hotkey, row.validatorName?.trim()?.takeIf { it.isNotBlank() } ?: address)
}

internal fun mapBittensorDirectoryPreview(chain: Chain, row: BittensorValidatorsRemote.Item): BittensorValidator? {
    val address = row.hotkey ?: return null
    val hotkey = mapBittensorPreviewHotkey(chain, address) ?: return null
    val name = row.identitySourceName?.trim()?.takeIf { it.isNotBlank() }
        ?: row.stakeSourceName?.trim()?.takeIf { it.isNotBlank() }
        ?: address
    return BittensorValidator(hotkey, name)
}

private fun mapBittensorPreviewHotkey(chain: Chain, address: String): AccountIdKey? {
    return try {
        AccountIdKey(chain.accountIdOf(address))
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        null
    }
}
