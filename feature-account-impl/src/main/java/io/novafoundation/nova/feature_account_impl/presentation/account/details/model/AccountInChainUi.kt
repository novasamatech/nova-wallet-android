package io.novafoundation.nova.feature_account_impl.presentation.account.details.model

import android.graphics.drawable.Drawable
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi

class AccountInChainUi(
    val chainUi: ChainUi,
    val addressOrHint: String,
    val address: String?,
    val accountIcon: Drawable
)
