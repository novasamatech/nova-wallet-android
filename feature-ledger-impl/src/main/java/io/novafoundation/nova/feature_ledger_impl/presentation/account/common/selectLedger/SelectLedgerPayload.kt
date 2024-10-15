package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger

import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.parcelize.Parcelize

@Parcelize
class SelectLedgerPayload(
    val chainId: ChainId
) : Parcelable
