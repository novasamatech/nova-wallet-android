package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect

import android.os.Parcelable
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import kotlinx.android.parcel.Parcelize

@Parcelize
class ParitySignerAccountPayload(
    val accountId: ByteArray,
    val variant: PolkadotVaultVariant
) : Parcelable
