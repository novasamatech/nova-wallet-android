package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize

interface SelectLedgerAddressInterScreenRequester: InterScreenRequester<SelectLedgerPayload, SelectLedgerAddressInterScreenCommunicator.Response>

interface SelectLedgerAddressInterScreenResponder: InterScreenResponder<SelectLedgerPayload, SelectLedgerAddressInterScreenCommunicator.Response>

interface SelectLedgerAddressInterScreenCommunicator: SelectLedgerAddressInterScreenRequester, SelectLedgerAddressInterScreenResponder  {

    @Parcelize
    class Response(
        val publicKey: ByteArray,
        val address: String,
        val chainId: ChainId
    ): Parcelable
}
