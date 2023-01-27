package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list.model

import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateIcon
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateStatsModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateTypeModel
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class DelegateListModel(
    val icon: DelegateIcon,
    val accountId: AccountId,
    val name: String,
    val type: DelegateTypeModel?,
    val description: String?,
    val stats: DelegateStatsModel
)
