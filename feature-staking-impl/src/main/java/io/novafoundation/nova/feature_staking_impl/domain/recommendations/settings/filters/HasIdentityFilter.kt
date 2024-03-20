package io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.filters

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.hasModule
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationFilter
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot

object HasIdentityFilter : RecommendationFilter {

    override fun shouldInclude(model: Validator): Boolean {
        return model.identity != null
    }

    override fun availableIn(runtime: RuntimeSnapshot): Boolean {
        return runtime.metadata.hasModule(Modules.IDENTITY)
    }
}
