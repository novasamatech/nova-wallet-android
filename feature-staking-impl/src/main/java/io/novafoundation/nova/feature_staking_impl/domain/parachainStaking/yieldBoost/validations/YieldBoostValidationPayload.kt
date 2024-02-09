package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations

import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostConfiguration
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostTask
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee

class YieldBoostValidationPayload(
    val collator: Collator,
    val activeTasks: List<YieldBoostTask>,
    val configuration: YieldBoostConfiguration,
    val asset: Asset,
    val fee: DecimalFee,
)
