package jp.co.soramitsu.feature_account_impl.presentation.exporting.json.confirm

import android.os.Parcelable
import jp.co.soramitsu.feature_account_impl.presentation.exporting.ExportPayload
import kotlinx.android.parcel.Parcelize

@Parcelize
class ExportJsonConfirmPayload(val exportPayload: ExportPayload, val json: String) : Parcelable
