package io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import kotlinx.parcelize.Parcelize

interface RestoreBackupPasswordRequester :
    InterScreenRequester<RestoreBackupPasswordRequester.EmptyRequest, RestoreBackupPasswordResponder.Success> {

    @Parcelize
    object EmptyRequest : Parcelable
}

interface RestoreBackupPasswordResponder :
    InterScreenResponder<RestoreBackupPasswordRequester.EmptyRequest, RestoreBackupPasswordResponder.Success> {

    @Parcelize
    object Success : Parcelable
}

interface RestoreBackupPasswordCommunicator : RestoreBackupPasswordRequester, RestoreBackupPasswordResponder
