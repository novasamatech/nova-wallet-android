package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show

import android.os.Parcelable
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator
import kotlinx.android.parcel.Parcelize

@Parcelize
class ShowSignParitySignerPayload(
    val request: SignInterScreenCommunicator.Request,
    val polkadotVaultVariant: PolkadotVaultVariant,
) : Parcelable
