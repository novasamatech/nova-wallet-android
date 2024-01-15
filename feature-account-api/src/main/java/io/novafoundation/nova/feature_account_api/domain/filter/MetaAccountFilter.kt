package io.novafoundation.nova.feature_account_api.domain.filter

import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

class MetaAccountFilter(val mode: Mode, val metaIds: List<Long>) : Filter<MetaAccount> {

    enum class Mode {
        INCLUDE,
        EXCLUDE
    }

    override fun shouldInclude(model: MetaAccount): Boolean {
        return when (mode) {
            Mode.INCLUDE -> metaIds.contains(model.id)
            Mode.EXCLUDE -> !metaIds.contains(model.id)
        }
    }
}
