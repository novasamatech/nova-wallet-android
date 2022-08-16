package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountWithTotalBalance

interface MetaAccountSelectRules {
    suspend fun select(metaAccountWithBalance: MetaAccountWithTotalBalance): Boolean
}
