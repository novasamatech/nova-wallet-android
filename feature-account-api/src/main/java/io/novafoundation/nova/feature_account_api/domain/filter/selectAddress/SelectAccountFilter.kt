package io.novafoundation.nova.feature_account_api.domain.filter.selectAddress

import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.isControllableWallet

sealed interface SelectAccountFilter : Filter<MetaAccount> {

    class Everything : SelectAccountFilter {

        override fun shouldInclude(model: MetaAccount): Boolean {
            return true
        }
    }

    class ControllableWallets() : SelectAccountFilter {

        override fun shouldInclude(model: MetaAccount): Boolean {
            return model.type.isControllableWallet()
        }
    }

    class ExcludeMetaAccounts(val metaIds: List<Long>) : SelectAccountFilter {

        override fun shouldInclude(model: MetaAccount): Boolean {
            return !metaIds.contains(model.id)
        }
    }
}
