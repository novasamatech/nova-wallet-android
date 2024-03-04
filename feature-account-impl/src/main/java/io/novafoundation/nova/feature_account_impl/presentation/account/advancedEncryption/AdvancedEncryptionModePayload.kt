package io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption

import android.os.Parcelable
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize

sealed class AdvancedEncryptionModePayload : Parcelable {

    @Parcelize
    class Change(val addAccountPayload: AddAccountPayload) : AdvancedEncryptionModePayload()

    @Parcelize
    class View(
        val metaAccountId: Long,
        val chainId: ChainId,
        val hideDerivationPaths: Boolean = false
    ) : AdvancedEncryptionModePayload()
}
