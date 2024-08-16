package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import kotlinx.android.parcel.Parcelize

interface SelectLedgerAddressInterScreenRequester : InterScreenRequester<SelectLedgerPayload, LedgerChainAccount>

interface SelectLedgerAddressInterScreenResponder : InterScreenResponder<SelectLedgerPayload, LedgerChainAccount>

interface SelectLedgerAddressInterScreenCommunicator : SelectLedgerAddressInterScreenRequester, SelectLedgerAddressInterScreenResponder

@Parcelize
class LedgerChainAccount(
    val publicKey: ByteArray,
    val encryptionType: EncryptionType,
    val address: String,
    val chainId: ChainId,
    val derivationPath: String,
) : Parcelable
