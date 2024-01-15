package io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress

import io.novafoundation.nova.common.utils.EverythingFilter
import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.feature_account_api.domain.filter.selectAddress.SelectAddressAccountFilter
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressRequester.Request

fun Request.Filter.toMetaAccountsFilter(): SelectAddressAccountFilter {
    return when (this) {
        is Request.Filter.Everything -> SelectAddressAccountFilter.Everything()

        is Request.Filter.ExcludeMetaIds -> SelectAddressAccountFilter.ExcludeMetaAccounts(this.metaIds)
    }
}

fun SelectAddressAccountFilter.toRequestFilter(): Request.Filter {
    return when (this) {
        is SelectAddressAccountFilter.Everything -> Request.Filter.Everything

        is SelectAddressAccountFilter.ExcludeMetaAccounts -> Request.Filter.ExcludeMetaIds(this.metaIds)
    }
}
