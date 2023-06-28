package io.novafoundation.nova

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboard
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.isSyncing
import kotlinx.coroutines.flow.launchIn
import org.junit.Test
import java.lang.reflect.Type

class StakingDashboardIntegrationTest: BaseIntegrationTest() {

    private val stakingApi = FeatureUtils.getFeature<StakingFeatureApi>(context, StakingFeatureApi::class.java)

    private val interactor = stakingApi.dashboardInteractor

    private val updateSystem = stakingApi.dashboardUpdateSystem

    private val gson = GsonBuilder()
        .registerTypeHierarchyAdapter(AggregatedStakingDashboardOption::class.java, AggregatedStakingDashboardOptionDesirializer())
        .create()

    @Test
    fun syncStakingDashboard() = runTest {
        updateSystem.start()
            .inBackground()
            .launchIn(this)

        interactor.stakingDashboardFlow()
            .inBackground()
            .collect(::logDashboard)
    }

    private fun logDashboard(dashboard: ExtendedLoadingState<StakingDashboard>) {
        if (dashboard !is ExtendedLoadingState.Loaded) return

        val serialized = gson.toJson(dashboard)

        val message = """
            Dashboard state:
                Syncing items: ${dashboard.data.syncingItemsCount()}
                $serialized
        """.trimIndent()

        Log.d("StakingDashboardIntegrationTest", message)
    }

    private class AggregatedStakingDashboardOptionDesirializer : JsonSerializer<AggregatedStakingDashboardOption<*>> {
        override fun serialize(src: AggregatedStakingDashboardOption<*>, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
           return JsonObject().apply {
               add("chain", JsonPrimitive(src.chain.name))
               add("stakingState", context.serialize(src.stakingState))
               add("syncing", context.serialize(src.syncingStage))
           }
        }
    }

    private fun StakingDashboard.syncingItemsCount(): Int {
        return withoutStake.count { it.syncingStage.isSyncing() } + hasStake.count { it.syncingStage.isSyncing() }
    }
}
