package io.novafoundation.nova.feature_multisig_operations.presentation.details.general

import android.os.Parcelable
import io.novafoundation.nova.feature_multisig_operations.presentation.common.MultisigOperationPayload
import kotlinx.parcelize.Parcelize

@Parcelize
class MultisigOperationDetailsPayload(
    val operation: MultisigOperationPayload,
    val navigationButtonMode: NavigationButtonMode = NavigationButtonMode.BACK
) : Parcelable {
    enum class NavigationButtonMode {
        BACK, CLOSE
    }
}
