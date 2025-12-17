package io.novafoundation.nova.feature_multisig_operations.presentation.details.general.adapter

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountModel

data class SignatoryRvItem(
    val accountModel: AccountModel,
    val subtitle: CharSequence?,
    val isApproved: Boolean,
)
