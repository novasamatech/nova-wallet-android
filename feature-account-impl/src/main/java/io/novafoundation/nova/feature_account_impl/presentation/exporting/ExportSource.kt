package io.novafoundation.nova.feature_account_impl.presentation.exporting

import androidx.annotation.StringRes
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.common.accountSource.AccountSource

sealed class ExportSource(@StringRes nameRes: Int) : AccountSource(nameRes) {
    object Json : ExportSource(R.string.recovery_json)

    object Mnemonic : ExportSource(R.string.recovery_passphrase)

    object Seed : ExportSource(R.string.recovery_raw_seed)
}
