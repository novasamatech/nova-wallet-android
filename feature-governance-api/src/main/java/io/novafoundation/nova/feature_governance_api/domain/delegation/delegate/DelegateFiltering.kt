package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate

import io.novafoundation.nova.common.utils.Filter

enum class DelegateFiltering : Filter<Delegate> {

    ALL_ACCOUNTS {
        override fun shouldInclude(model: Delegate): Boolean {
            return true
        }
    },

    ORGANIZATIONS {
        override fun shouldInclude(model: Delegate): Boolean {
            val accountType = model.metadata?.accountType ?: return false

            return accountType == DelegateAccountType.ORGANIZATION
        }
    },

    INDIVIDUALS {
        override fun shouldInclude(model: Delegate): Boolean {
            val accountType = model.metadata?.accountType ?: return false

            return accountType == DelegateAccountType.INDIVIDUAL
        }
    }
}
