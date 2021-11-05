package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm

import android.os.Parcelable
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import kotlinx.android.parcel.Parcelize

@Parcelize
class ConfirmMnemonicPayload(
    val mnemonic: List<String>,
    val createExtras: CreateExtras?
) : Parcelable {
    @Parcelize
    class CreateExtras(
        val accountName: String,
        val cryptoType: CryptoType,
        val addAccountPayload: AddAccountPayload,
        val derivationPath: String
    ) : Parcelable
}
