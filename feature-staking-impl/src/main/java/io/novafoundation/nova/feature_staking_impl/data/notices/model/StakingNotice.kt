package io.novafoundation.nova.feature_staking_impl.data.notices.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.time.LocalDate

data class StakingNotice(
    val chainId: ChainId,
    val severity: Severity,
    val shortText: String,   // already locale-resolved
    val longText: String,    // already locale-resolved
    val endDate: LocalDate?
) {
    enum class Severity { INFO, CRITICAL }
}
