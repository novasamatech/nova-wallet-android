package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class CreateCloudBackupPasswordPayload(
    val walletName: String
) : Parcelable
