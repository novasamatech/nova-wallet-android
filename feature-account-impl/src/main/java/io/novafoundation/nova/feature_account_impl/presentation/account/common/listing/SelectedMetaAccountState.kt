package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

sealed interface SelectedMetaAccountState {

    fun isSelected(metaAccount: MetaAccount): Boolean

    object CurrentlySelected : SelectedMetaAccountState {

        override fun isSelected(metaAccount: MetaAccount): Boolean {
            return metaAccount.isSelected
        }
    }

    data class Specified(val ids: Set<Long>) : SelectedMetaAccountState {

        override fun isSelected(metaAccount: MetaAccount): Boolean {
            return ids.contains(metaAccount.id)
        }
    }
}
