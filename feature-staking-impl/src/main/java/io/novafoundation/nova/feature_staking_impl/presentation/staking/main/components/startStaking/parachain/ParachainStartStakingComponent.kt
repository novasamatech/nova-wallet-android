package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.parachain

import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer.Payload.*
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.formatFractionAsPercentage
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.welcome.ParachainStakingWelcomeValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.welcome.ParachainStakingWelcomeValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.welcome.ParachainStakingWelcomeValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.welcome.hasEthereumAccount
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.maximumAnnualApr
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.StakingRewardEstimationBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.BaseStartStakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingEvent
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class ParachainStartStakingComponentFactory(
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val rewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
    private val resourceManager: ResourceManager,
    private val router: ParachainStakingRouter,
    private val validationExecutor: ValidationExecutor
) {

    private val validationSystem: ParachainStakingWelcomeValidationSystem by lazy(LazyThreadSafetyMode.NONE) {
        ValidationSystem {
            hasEthereumAccount()
        }
    }

    fun create(
        assetWithChain: SingleAssetSharedState.AssetWithChain,
        hostContext: ComponentHostContext
    ): StartStakingComponent = ParachainStartStakingComponent(
        delegatorStateUseCase = delegatorStateUseCase,
        rewardCalculatorFactory = rewardCalculatorFactory,
        resourceManager = resourceManager,
        router = router,
        assetWithChain = assetWithChain,
        hostContext = hostContext,
        validationSystem = validationSystem,
        validationExecutor = validationExecutor
    )
}

private class ParachainStartStakingComponent(
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val rewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
    private val resourceManager: ResourceManager,
    private val router: ParachainStakingRouter,

    private val validationSystem: ParachainStakingWelcomeValidationSystem,
    private val validationExecutor: ValidationExecutor,
    private val assetWithChain: SingleAssetSharedState.AssetWithChain,
    private val hostContext: ComponentHostContext,
) : BaseStartStakingComponent(assetWithChain, hostContext, resourceManager) {

    private val rewardCalculator = async { rewardCalculatorFactory.create(assetWithChain.chain.id) }

    private val delegatorStateFlow = hostContext.selectedAccount.flatMapLatest {
        delegatorStateUseCase.delegatorStateFlow(it, assetWithChain.chain, assetWithChain.asset)
    }.shareInBackground()

    override suspend fun maxPeriodReturnPercentage(days: Int): BigDecimal {
        return rewardCalculator().maximumGain(days)
    }

    override val isComponentApplicable = delegatorStateFlow.map { it is DelegatorState.None }

    override suspend fun infoClicked() = withContext(Dispatchers.Default) {
        val rewardCalculator = rewardCalculator()

        val payload = StakingRewardEstimationBottomSheet.Payload(
            max = rewardCalculator.maximumAnnualApr().formatFractionAsPercentage(),
            average = rewardCalculator.averageApr().formatFractionAsPercentage(),
            returnsTypeFormat = R.string.staking_apr,
            title = resourceManager.getString(R.string.staking_reward_info_title_transferrable)
        )
        val showRewardEstimationDetails = StartStakingEvent.ShowRewardEstimationDetails(payload)

        events.postValue(showRewardEstimationDetails.event())
    }

    override suspend fun nextClicked() {
        val payload = ParachainStakingWelcomeValidationPayload(
            account = hostContext.selectedAccount.first(),
            chain = assetWithChain.chain
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            errorDisplayer = hostContext.errorDisplayer,
            validationFailureTransformerCustom = { status, _ -> validationFailure(status.reason)  }
        ) {
            router.openStartStaking()
        }
    }

    private fun validationFailure(failure: ParachainStakingWelcomeValidationFailure): TransformedFailure.Custom {
        return when(failure) {
            is ParachainStakingWelcomeValidationFailure.MissingEthereumAccount -> {
                TransformedFailure.Custom(
                    dialogPayload = CustomDialogDisplayer.Payload(
                        title = resourceManager.getString(R.string.common_missing_account_title, failure.chain.name),
                        message = resourceManager.getString(R.string.staking_missing_account_message, failure.chain.name),
                        okAction = DialogAction(
                            title = resourceManager.getString(R.string.common_add),
                            action = { router.openAddAccount(failure.chain.id, failure.metaAccount.id) }
                        ),
                        cancelAction = DialogAction.noOp(resourceManager.getString(R.string.common_cancel))
                    )
                )
            }
        }
    }
}
