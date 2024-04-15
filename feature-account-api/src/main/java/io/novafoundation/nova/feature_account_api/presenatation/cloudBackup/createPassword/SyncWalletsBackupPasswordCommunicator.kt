package io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import kotlinx.android.parcel.Parcelize

interface SyncWalletsBackupPasswordRequester :
    InterScreenRequester<SyncWalletsBackupPasswordRequester.EmptyRequest, SyncWalletsBackupPasswordResponder.Response> {

    @Parcelize
    object EmptyRequest : Parcelable
}

interface SyncWalletsBackupPasswordResponder :
    InterScreenResponder<SyncWalletsBackupPasswordRequester.EmptyRequest, SyncWalletsBackupPasswordResponder.Response> {

    @Parcelize
    class Response(val isSyncingSuccessful: Boolean) : Parcelable
}

interface SyncWalletsBackupPasswordCommunicator : SyncWalletsBackupPasswordRequester, SyncWalletsBackupPasswordResponder
