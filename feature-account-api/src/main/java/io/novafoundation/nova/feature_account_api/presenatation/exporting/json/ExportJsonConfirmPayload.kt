package io.novafoundation.nova.feature_account_api.presenatation.exporting.json

import android.os.Parcelable
import io.novafoundation.nova.feature_account_api.presenatation.exporting.ExportPayload
import kotlinx.android.parcel.Parcelize

@Parcelize
class ExportJsonConfirmPayload(val exportPayload: ExportPayload, val json: String) : Parcelable
