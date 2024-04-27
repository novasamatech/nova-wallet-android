package io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.postprocessors

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.hasModule
import io.novafoundation.nova.feature_account_api.data.model.ChildIdentity
import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novafoundation.nova.feature_account_api.data.model.RootIdentity
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationPostProcessor
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot

private const val MAX_PER_CLUSTER = 2

object RemoveClusteringPostprocessor : RecommendationPostProcessor {

    override fun apply(original: List<Validator>): List<Validator> {
        val clusterCounter = mutableMapOf<OnChainIdentity, Int>()

        return original.filter { validator ->
            if (validator.shouldSkipClusteringFiltering()) return@filter true

            validator.clusterIdentity()?.let {
                val currentCounter = clusterCounter.getOrDefault(it, 0)

                clusterCounter[it] = currentCounter + 1

                currentCounter < MAX_PER_CLUSTER
            } ?: true
        }
    }

    override fun availableIn(runtime: RuntimeSnapshot): Boolean {
        return runtime.metadata.hasModule(Modules.IDENTITY)
    }

    private fun Validator.clusterIdentity(): OnChainIdentity? {
        return when (val validatorIdentity = identity) {
            is RootIdentity -> validatorIdentity
            is ChildIdentity -> validatorIdentity.parentIdentity
            else -> null
        }
    }

    private fun Validator.shouldSkipClusteringFiltering(): Boolean {
        return isNovaValidator
    }
}
