package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger.SelectLedgerLegacyPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import kotlinx.parcelize.Parcelize

interface SelectLedgerAddressInterScreenRequester : InterScreenRequester<SelectLedgerLegacyPayload, LedgerChainAccount>

interface SelectLedgerAddressInterScreenResponder : InterScreenResponder<SelectLedgerLegacyPayload, LedgerChainAccount>

interface SelectLedgerAddressInterScreenCommunicator : SelectLedgerAddressInterScreenRequester, SelectLedgerAddressInterScreenResponder

@Parcelize
class LedgerChainAccount(
    val publicKey: ByteArray,
    val encryptionType: EncryptionType,
    val address: String,
    val chainId: ChainId,
    val derivationPath: String,
) : Parcelable
