package io.novafoundation.nova.feature_account_impl.presentation.exporting.json.confirm

import android.os.Parcelable
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import kotlinx.android.parcel.Parcelize

@Parcelize
class ExportJsonConfirmPayload(val exportPayload: io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload, val json: String) : Parcelable
