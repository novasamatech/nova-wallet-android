package io.novafoundation.nova.feature_staking_impl.presentation.confirm.hints

import io.novafoundation.nova.common.mixin.hints.ConstantHintsMixin
import io.novafoundation.nova.common.mixin.hints.HintsMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess.ReadyToSubmit.Payload
import kotlinx.coroutines.CoroutineScope

class ConfirmStakeHintsMixinFactory(
    private val interactor: StakingInteractor,
    private val resourceManager: ResourceManager,
) {

    fun create(
        coroutineScope: CoroutineScope,
        payload: Payload
    ): HintsMixin = ConfirmStakeHintsMixin(
        interactor = interactor,
        resourceManager = resourceManager,
        payload = payload,
        coroutineScope = coroutineScope
    )
}

private class ConfirmStakeHintsMixin(
    private val interactor: StakingInteractor,
    private val resourceManager: ResourceManager,
    private val payload: Payload,
    coroutineScope: CoroutineScope
) : ConstantHintsMixin(coroutineScope) {

    override suspend fun getHints(): List<String> {

        return when (payload) {
            is Payload.Validators -> changeValidatorsHints()
            else -> beginStakeHints()
        }
    }

    private suspend fun beginStakeHints(): List<String> = listOf(
        rewardPeriodHint(),
        noRewardDurationUnstakingHint(),
        redeemHint(),
        unstakingDurationHint(),
    )

    private fun changeValidatorsHints(): List<String> = listOf(
        validatorsChangeHint()
    )

    private fun redeemHint(): String {
        return resourceManager.getString(R.string.staking_hint_redeem_v2_2_0)
    }

    private fun validatorsChangeHint(): String {
        return resourceManager.getString(R.string.staking_your_validators_changing_title)
    }

    private fun noRewardDurationUnstakingHint(): String {
        return resourceManager.getString(R.string.staking_hint_no_rewards_v2_2_0)
    }

    private suspend fun unstakingDurationHint(): String {
        val lockupPeriod = interactor.getLockupPeriodInDays()

        return resourceManager.getString(
            R.string.staking_hint_unstake_format_v2_2_0,
            resourceManager.getQuantityString(R.plurals.staking_main_lockup_period_value, lockupPeriod, lockupPeriod)
        )
    }

    private suspend fun rewardPeriodHint(): String {
        val hours = interactor.getEraHoursLength()

        return resourceManager.getString(
            R.string.staking_hint_rewards_format_v2_2_0,
            resourceManager.getQuantityString(R.plurals.common_hours_format, hours, hours)
        )
    }
}
