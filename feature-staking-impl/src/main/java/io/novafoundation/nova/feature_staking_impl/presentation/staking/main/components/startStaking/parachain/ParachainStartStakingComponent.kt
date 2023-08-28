package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.parachain

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.formatting.formatFractionAsPercentage
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.validation.handleChainAccountNotFound
import io.novafoundation.nova.feature_account_api.domain.validation.hasChainAccount
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.welcome.ParachainStakingWelcomeValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.welcome.ParachainStakingWelcomeValidationFailure.MissingEthereumAccount
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.welcome.ParachainStakingWelcomeValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.welcome.ParachainStakingWelcomeValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.maximumAnnualApr
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.openStartStaking
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.common.StartParachainStakingMode
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.StakingRewardEstimationBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.BaseStartStakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingEvent
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
            hasChainAccount(
                chain = { it.chain },
                metaAccount = { it.account },
                error = ::MissingEthereumAccount
            )
        }
    }

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext
    ): StartStakingComponent = ParachainStartStakingComponent(
        delegatorStateUseCase = delegatorStateUseCase,
        rewardCalculatorFactory = rewardCalculatorFactory,
        resourceManager = resourceManager,
        router = router,
        stakingOption = stakingOption,
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
    private val stakingOption: StakingOption,
    private val hostContext: ComponentHostContext,
) : BaseStartStakingComponent(stakingOption, hostContext, resourceManager) {

    private val rewardCalculator = async { rewardCalculatorFactory.create(stakingOption) }

    private val delegatorStateFlow = hostContext.selectedAccount.flatMapLatest {
        delegatorStateUseCase.delegatorStateFlow(it, stakingOption.assetWithChain.chain, stakingOption.assetWithChain.asset)
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
            chain = stakingOption.assetWithChain.chain
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            errorDisplayer = hostContext.errorDisplayer,
            validationFailureTransformerCustom = { status, _ -> validationFailure(status.reason) }
        ) {
            router.openStartStaking(StartParachainStakingMode.START)
        }
    }

    private fun validationFailure(failure: ParachainStakingWelcomeValidationFailure): TransformedFailure {
        return when (failure) {
            is MissingEthereumAccount -> handleChainAccountNotFound(
                failure = failure,
                resourceManager = resourceManager,
                goToWalletDetails = { router.openWalletDetails(failure.account.id) },
                addAccountDescriptionRes = R.string.staking_missing_account_message
            )
        }
    }
}
