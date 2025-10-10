package io.novafoundation.nova.feature_staking_impl.presentation.staking.main

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_ahm_api.domain.ChainMigrationInfoUseCase
import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfig
import io.novafoundation.nova.feature_ahm_api.presentation.getChainMigrationDateFormat
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.YourPoolComponentFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.runtime.state.selectedAssetFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val STAKING_MIGRATION_INFO = "STAKING_MIGRATION_INFO"

class StakingViewModel(
    selectedAccountUseCase: SelectedAccountUseCase,

    assetUseCase: AssetUseCase,
    alertsComponentFactory: AlertsComponentFactory,
    unbondingComponentFactory: UnbondingComponentFactory,
    stakeSummaryComponentFactory: StakeSummaryComponentFactory,
    userRewardsComponentFactory: UserRewardsComponentFactory,
    stakeActionsComponentFactory: StakeActionsComponentFactory,
    networkInfoComponentFactory: NetworkInfoComponentFactory,
    yourPoolComponentFactory: YourPoolComponentFactory,

    val router: StakingRouter,

    private val validationExecutor: ValidationExecutor,
    private val stakingSharedState: StakingSharedState,
    private val resourceManager: ResourceManager,
    private val externalActionsMixin: ExternalActions.Presentation,
    private val chainMigrationInfoUseCase: ChainMigrationInfoUseCase,
    stakingUpdateSystem: UpdateSystem,
) : BaseViewModel(),
    Validatable by validationExecutor,
    ExternalActions by externalActionsMixin,
    Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    private val selectedAssetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    val titleFlow = stakingSharedState.selectedAssetFlow()
        .map { resourceManager.getString(R.string.staking_title_format, it.name) }
        .shareInBackground()

    private val selectedAccountFlow = selectedAccountUseCase.selectedMetaAccountFlow()
        .shareInBackground()

    private val componentHostContext = ComponentHostContext(
        errorDisplayer = ::showError,
        selectedAccount = selectedAccountFlow,
        assetFlow = selectedAssetFlow,
        scope = this,
        validationExecutor = validationExecutor,
        externalActions = externalActionsMixin
    )

    val unbondingComponent = unbondingComponentFactory.create(componentHostContext)
    val stakeSummaryComponent = stakeSummaryComponentFactory.create(componentHostContext)
    val userRewardsComponent = userRewardsComponentFactory.create(componentHostContext)
    val stakeActionsComponent = stakeActionsComponentFactory.create(componentHostContext)
    val networkInfoComponent = networkInfoComponentFactory.create(componentHostContext)
    val alertsComponent = alertsComponentFactory.create(componentHostContext)
    val yourPoolComponent = yourPoolComponentFactory.create(componentHostContext)

    private val dateFormatter = getChainMigrationDateFormat()

    val migrationAlertFlow = selectedAssetFlow.flatMapLatest {
        val chainAsset = it.token.configuration
        combine(
            chainMigrationInfoUseCase.observeMigrationConfigOrNull(chainAsset.chainId, chainAsset.id),
            chainMigrationInfoUseCase.observeInfoShouldBeHidden(STAKING_MIGRATION_INFO, chainAsset.chainId, chainAsset.id)
        ) { configWithChains, shouldBeHidden ->
            if (shouldBeHidden) return@combine null
            if (configWithChains == null) return@combine null

            val config = configWithChains.config
            val destinationAsset = configWithChains.destinationAsset
            val destinationChain = configWithChains.destinationChain
            val formattedDate = dateFormatter.format(config.timeStartAt)
            AlertModel(
                style = AlertView.Style.fromPreset(AlertView.StylePreset.INFO),
                message = resourceManager.getString(
                    R.string.staking_details_migration_alert_title,
                    destinationAsset.name,
                    destinationChain.name,
                    formattedDate
                ),
                linkAction = AlertModel.ActionModel(resourceManager.getString(R.string.common_learn_more)) { learnMoreMigrationClicked(config) },
            )
        }
    }.shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun closeMigrationAlert() {
        launch {
            val chainAsset = selectedAssetFlow.first().token.configuration
            chainMigrationInfoUseCase.markMigrationInfoAsHidden(STAKING_MIGRATION_INFO, chainAsset.chainId, chainAsset.id)
        }
    }

    init {
        stakingUpdateSystem.start()
            .launchIn(this)
    }

    private fun learnMoreMigrationClicked(config: ChainMigrationConfig) {
        launch {
            openBrowserEvent.value = Event(config.wikiURL)
        }
    }
}
