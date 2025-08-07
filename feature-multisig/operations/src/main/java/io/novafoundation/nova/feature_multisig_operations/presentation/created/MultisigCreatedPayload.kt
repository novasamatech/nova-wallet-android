package io.novafoundation.nova.feature_multisig_operations.presentation.created

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class MultisigCreatedPayload(
    val walletWasSwitched: Boolean
) : Parcelable
