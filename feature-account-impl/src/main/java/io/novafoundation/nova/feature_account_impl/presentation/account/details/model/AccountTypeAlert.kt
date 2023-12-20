package io.novafoundation.nova.feature_account_impl.presentation.account.details.model

import io.novafoundation.nova.common.view.AlertView

class AccountTypeAlert(
    val style: AlertView.Style,
    val message: String,
    val subMessage: CharSequence? = null
)
