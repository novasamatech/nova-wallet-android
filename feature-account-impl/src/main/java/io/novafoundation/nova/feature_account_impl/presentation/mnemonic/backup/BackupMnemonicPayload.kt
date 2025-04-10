package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup

import android.os.Parcelable
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.parcelize.Parcelize

sealed class BackupMnemonicPayload : Parcelable {

    @Parcelize
    class Create(
        val newWalletName: String?,
        val addAccountPayload: AddAccountPayload
    ) : BackupMnemonicPayload()

    @Parcelize
    class Confirm(
        val chainId: ChainId,
        val metaAccountId: Long
    ) : BackupMnemonicPayload()
}
