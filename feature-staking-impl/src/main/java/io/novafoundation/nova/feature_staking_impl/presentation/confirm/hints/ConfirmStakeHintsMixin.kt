package io.novafoundation.nova.feature_staking_impl.presentation.confirm.hints

import io.novafoundation.nova.common.mixin.hints.ConstantHintsMixin
import io.novafoundation.nova.common.mixin.hints.HintsMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess.ReadyToSubmit.Payload
import io.novafoundation.nova.feature_staking_impl.presentation.common.hints.StakingHintsUseCase
import kotlinx.coroutines.CoroutineScope

class ConfirmStakeHintsMixinFactory(
    private val interactor: StakingInteractor,
    private val resourceManager: ResourceManager,
    private val stakingHintsUseCase: StakingHintsUseCase,
) {

    fun create(
        coroutineScope: CoroutineScope,
        payload: Payload
    ): HintsMixin = ConfirmStakeHintsMixin(
        interactor = interactor,
        resourceManager = resourceManager,
        payload = payload,
        coroutineScope = coroutineScope,
        stakingHintsUseCase = stakingHintsUseCase
    )
}

private class ConfirmStakeHintsMixin(
    private val interactor: StakingInteractor,
    private val resourceManager: ResourceManager,
    private val payload: Payload,
    private val stakingHintsUseCase: StakingHintsUseCase,
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
        stakingHintsUseCase.noRewardDurationUnstakingHint(),
        stakingHintsUseCase.redeemHint(),
        stakingHintsUseCase.unstakingDurationHint(coroutineScope),
    )

    private fun changeValidatorsHints(): List<String> = listOf(
        validatorsChangeHint()
    )

    private fun validatorsChangeHint(): String {
        return resourceManager.getString(R.string.staking_your_validators_changing_title)
    }

    private suspend fun rewardPeriodHint(): String {
        val eraDuration = interactor.getEraDuration(coroutineScope)
        val formattedDuration = resourceManager.formatDuration(eraDuration)

        return resourceManager.getString(R.string.staking_hint_rewards_format_v2_2_0, formattedDuration)
    }
}
