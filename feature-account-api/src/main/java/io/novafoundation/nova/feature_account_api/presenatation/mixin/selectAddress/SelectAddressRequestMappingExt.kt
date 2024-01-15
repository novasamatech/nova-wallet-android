package io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress

import io.novafoundation.nova.common.utils.EverythingFilter
import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.feature_account_api.domain.filter.MetaAccountFilter
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressRequester

fun SelectAddressRequester.Request.Filter.toMetaAccountsFilter(): Filter<MetaAccount> {
    return when (this) {
        is SelectAddressRequester.Request.Filter.Everything -> EverythingFilter()

        is SelectAddressRequester.Request.Filter.ExcludeMetaIds -> MetaAccountFilter(
            MetaAccountFilter.Mode.EXCLUDE,
            this.metaIds
        )
    }
}
