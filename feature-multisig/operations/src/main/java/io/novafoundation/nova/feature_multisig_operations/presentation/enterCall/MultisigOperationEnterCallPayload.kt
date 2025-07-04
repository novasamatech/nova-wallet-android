package io.novafoundation.nova.feature_multisig_operations.presentation.enterCall

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class MultisigOperationEnterCallPayload(
    val operationId: String
) : Parcelable
