package io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import io.novafoundation.nova.common.utils.DialogExtensions
import io.novafoundation.nova.common.view.bottomSheet.ActionNotAllowedBottomSheet
import io.novafoundation.nova.feature_account_impl.R

class AcknowledgeSigningNotSupportedBottomSheet(
    context: Context,
    private val payload: SigningNotSupportedPresentable.Payload,
    private val onConfirm: () -> Unit
) : ActionNotAllowedBottomSheet(
    context = context,
    onSuccess = onConfirm,
),
    DialogExtensions {

    override val dialogInterface: DialogInterface
        get() = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title.setText(R.string.account_parity_signer_not_supported_title)
        subtitle.setText(payload.messageRes)

        applySolidIconStyle(payload.iconRes)
    }
}
