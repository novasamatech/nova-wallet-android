package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.createWallet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class CreateBackupPasswordPayload(
    val walletName: String
) : Parcelable
