package io.novafoundation.nova.feature_account_impl.presentation.mnemonic

import android.os.Parcelable
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.common.model.AdvancedEncryptionModel
import kotlinx.android.parcel.Parcelize

@Parcelize
class ConfirmMnemonicPayload(
    val mnemonic: List<String>,
    val createExtras: CreateExtras?
) : Parcelable {
    @Parcelize
    class CreateExtras(
        val accountName: String?,
        val addAccountPayload: AddAccountPayload,
        val advancedEncryptionModel: AdvancedEncryptionModel
    ) : Parcelable
}
