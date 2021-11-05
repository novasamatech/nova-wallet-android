package io.novafoundation.nova.feature_account_impl.presentation.account.mixin.api

import io.novafoundation.nova.feature_account_impl.presentation.account.model.LightMetaAccountUi
import kotlinx.coroutines.flow.Flow

interface AccountListingMixin {

    fun accountsFlow(): Flow<List<LightMetaAccountUi>>
}
