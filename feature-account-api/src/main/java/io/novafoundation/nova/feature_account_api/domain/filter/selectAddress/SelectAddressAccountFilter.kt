package io.novafoundation.nova.feature_account_api.domain.filter.selectAddress

import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

sealed interface SelectAddressAccountFilter : Filter<MetaAccount> {

    class Everything : SelectAddressAccountFilter {

        override fun shouldInclude(model: MetaAccount): Boolean {
            return true
        }
    }

    class ExcludeMetaAccounts(val metaIds: List<Long>) : SelectAddressAccountFilter {

        override fun shouldInclude(model: MetaAccount): Boolean {
            return !metaIds.contains(model.id)
        }
    }
}
