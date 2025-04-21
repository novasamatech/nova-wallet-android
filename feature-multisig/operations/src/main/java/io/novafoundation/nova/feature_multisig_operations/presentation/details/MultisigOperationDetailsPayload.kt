package io.novafoundation.nova.feature_multisig_operations.presentation.details

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class MultisigOperationDetailsPayload(
    val operationId: String
) : Parcelable
