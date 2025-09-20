package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.delegated

import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountListingMixin
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.delegated.DelegatedMetaAccountUpdatesListingMixin.FilterType
import kotlinx.coroutines.flow.Flow

interface DelegatedMetaAccountUpdatesListingMixin : MetaAccountListingMixin {

    sealed interface FilterType {
        data object Proxied : FilterType
        data object Multisig : FilterType

        data object Derivative : FilterType

        class UserIgnored(val overriddenFilter: FilterType) : FilterType
    }

    val accountTypeFilter: Flow<FilterType>

    fun filterBy(type: FilterType)
}

fun FilterType.filter(account: MetaAccount): Boolean = when (this) {
    FilterType.Proxied -> account.type == LightMetaAccount.Type.PROXIED
    FilterType.Multisig -> account.type == LightMetaAccount.Type.MULTISIG
    FilterType.Derivative -> account.type == LightMetaAccount.Type.DERIVATIVE
    is FilterType.UserIgnored -> overriddenFilter.filter(account)
}
