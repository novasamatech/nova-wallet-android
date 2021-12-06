package io.novafoundation.nova.feature_account_impl.presentation

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.data.mappers.mapAdvancedEncryptionResponseToAdvancedEncryption
import io.novafoundation.nova.feature_account_impl.data.mappers.mapAdvancedEncryptionStateToResponse
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryption
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionCommunicator.Response
import io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.AdvancedEncryptionPayload
import kotlinx.android.parcel.Parcelize

interface AdvancedEncryptionRequester : InterScreenRequester<AdvancedEncryptionPayload, Response>

suspend fun AdvancedEncryptionRequester.lastResponseOrDefault(addAccountPayload: AddAccountPayload, using: AdvancedEncryptionInteractor): Response {
    return latestResponse ?: mapAdvancedEncryptionStateToResponse(
        using.getInitialInputState(
            AdvancedEncryptionPayload.Change(addAccountPayload)
        )
    )
}

suspend fun AdvancedEncryptionRequester.lastAdvancedEncryptionOrDefault(
    addAccountPayload: AddAccountPayload,
    using: AdvancedEncryptionInteractor
): AdvancedEncryption {
    return mapAdvancedEncryptionResponseToAdvancedEncryption(lastResponseOrDefault(addAccountPayload, using))
}

interface AdvancedEncryptionResponder : InterScreenResponder<AdvancedEncryptionPayload, Response>

interface AdvancedEncryptionCommunicator : AdvancedEncryptionRequester, AdvancedEncryptionResponder {

    @Parcelize
    class Response(
        val substrateCryptoType: CryptoType?,
        val substrateDerivationPath: String?,
        val ethereumCryptoType: CryptoType?,
        val ethereumDerivationPath: String?
    ) : Parcelable
}
