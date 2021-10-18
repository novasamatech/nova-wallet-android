package jp.co.soramitsu.feature_account_impl.presentation.account.details

import android.graphics.drawable.Drawable
import jp.co.soramitsu.feature_account_api.presenatation.chain.ChainUi

class AccountInChainUi(
    val chainUi: ChainUi,
    val addressOrHint: String,
    val address: String?,
    val accountIcon: Drawable
)
