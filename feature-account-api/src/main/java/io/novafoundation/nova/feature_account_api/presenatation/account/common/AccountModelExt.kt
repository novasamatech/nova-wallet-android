package io.novafoundation.nova.feature_account_api.presenatation.account.common

import android.text.TextUtils
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountModel

fun AccountModel.relevantEllipsizeMode(): TextUtils.TruncateAt {
    return when (this) {
        is AccountModel.Address -> when (this.name) {
            // For address use middle
            null -> TextUtils.TruncateAt.MIDDLE

            // For name use end
            else -> TextUtils.TruncateAt.END
        }

        // For wallet always use end
        is AccountModel.Wallet -> TextUtils.TruncateAt.END
    }
}
