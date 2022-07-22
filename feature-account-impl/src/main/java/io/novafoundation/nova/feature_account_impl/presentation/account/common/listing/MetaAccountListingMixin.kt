package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import kotlinx.coroutines.flow.Flow

interface MetaAccountListingMixin {

    val metaAccountsFlow: Flow<List<Any>>
}
