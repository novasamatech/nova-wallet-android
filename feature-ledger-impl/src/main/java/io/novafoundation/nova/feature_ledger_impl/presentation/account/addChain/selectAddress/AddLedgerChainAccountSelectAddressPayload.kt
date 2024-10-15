package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectAddress

import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.parcelize.Parcelize

@Parcelize
class AddLedgerChainAccountSelectAddressPayload(
    val chainId: ChainId,
    val metaId: Long,
    val deviceId: String,
) : Parcelable
