package io.novafoundation.nova.feature_external_sign_api.model.signPayload

import android.os.Parcelable
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmSignPayload
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.polkadot.PolkadotSignPayload
import kotlinx.android.parcel.Parcelize

@Parcelize
class ExternalSignPayload(
    val signRequest: ExternalSignRequest,
    val dappMetadata: SigningDappMetadata?,
    val wallet: ExternalSignWallet
) : Parcelable

@Parcelize
class SigningDappMetadata(
    val icon: String?,
    val name: String?,
    val url: String
) : Parcelable

sealed class ExternalSignWallet : Parcelable {

    @Parcelize
    object Current : ExternalSignWallet()

    @Parcelize
    class WithId(val metaId: Long) : ExternalSignWallet()
}

sealed interface ExternalSignRequest : Parcelable {

    val id: String

    @Parcelize
    class Polkadot(override val id: String, val payload: PolkadotSignPayload) : ExternalSignRequest

    @Parcelize
    class Evm(override val id: String, val payload: EvmSignPayload) : ExternalSignRequest
}
