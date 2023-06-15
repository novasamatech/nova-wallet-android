package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking

import android.util.Log
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.formatFractionAsPercentage
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import java.math.BigDecimal

private const val PERIOD_MONTH = 30
private const val PERIOD_YEAR = 365

abstract class BaseStartStakingComponent(
    private val stakingOption: StakingOption,
    private val hostContext: ComponentHostContext,
    private val resourceManager: ResourceManager,
) : StartStakingComponent,
    CoroutineScope by hostContext.scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    abstract suspend fun maxPeriodReturnPercentage(days: Int): BigDecimal

    abstract val isComponentApplicable: Flow<Boolean>

    protected abstract suspend fun infoClicked()
    protected abstract suspend fun nextClicked()

    private val returnsFlow = flowOf {
        ReturnsModel(
            monthlyPercentage = maxPeriodReturnPercentage(PERIOD_MONTH).formatFractionAsPercentage(),
            yearlyPercentage = maxPeriodReturnPercentage(PERIOD_YEAR).formatFractionAsPercentage()
        )
    }

    override val events = MutableLiveData<Event<StartStakingEvent>>()

    // flow wrapper in order to not to leak this in constructor since class is abstract
    override val state: Flow<StartStakingState?> = flow {
        emitAll(constructStateFlow())
    }
        .onStart { emit(null) }
        .catch { Log.d(LOG_TAG, "Failed to construct state", it) }
        .shareInBackground(started = SharingStarted.Lazily)

    override fun onAction(action: StartStakingAction) {
        launch {
            when (action) {
                StartStakingAction.InfoClicked -> infoClicked()
                StartStakingAction.NextClicked -> nextClicked()
            }
        }
    }

    private fun constructStateFlow() = isComponentApplicable.transform { applicable ->
        if (applicable) {
            val inner = returnsFlow.withLoading().map {
                StartStakingState(
                    estimateEarningsTitle = estimateEarningsTitle(),
                    returns = it
                )
            }

            emitAll(inner)
        } else {
            emit(null)
        }
    }

    private fun estimateEarningsTitle(): String {
        val assetSymbol = stakingOption.assetWithChain.asset.symbol

        return resourceManager.getString(R.string.staking_estimate_earning_title_v2_2_0, assetSymbol)
    }
}
