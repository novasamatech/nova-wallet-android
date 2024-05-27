package io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import kotlinx.android.parcel.Parcelize

interface ChangeBackupPasswordRequester :
    InterScreenRequester<ChangeBackupPasswordRequester.EmptyRequest, ChangeBackupPasswordResponder.Success> {

    @Parcelize
    object EmptyRequest : Parcelable
}

interface ChangeBackupPasswordResponder :
    InterScreenResponder<ChangeBackupPasswordRequester.EmptyRequest, ChangeBackupPasswordResponder.Success> {

    @Parcelize
    object Success : Parcelable
}

interface ChangeBackupPasswordCommunicator : ChangeBackupPasswordRequester, ChangeBackupPasswordResponder
