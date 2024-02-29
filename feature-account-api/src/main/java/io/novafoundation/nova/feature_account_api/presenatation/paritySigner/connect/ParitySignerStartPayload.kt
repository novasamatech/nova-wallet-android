package io.novafoundation.nova.feature_account_api.presenatation.paritySigner.connect

import android.os.Parcelable
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import kotlinx.android.parcel.Parcelize

@Parcelize
class ParitySignerStartPayload(
    val variant: PolkadotVaultVariant
) : Parcelable
