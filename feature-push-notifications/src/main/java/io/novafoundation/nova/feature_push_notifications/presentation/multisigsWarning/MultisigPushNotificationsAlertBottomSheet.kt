package io.novafoundation.nova.feature_push_notifications.presentation.multisigsWarning

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_push_notifications.databinding.FragmentEnableMultisigPushesWarningBinding

open class MultisigPushNotificationsAlertBottomSheet(
    context: Context,
    private val onEnableClicked: () -> Unit,
) : BaseBottomSheet<FragmentEnableMultisigPushesWarningBinding>(context, R.style.BottomSheetDialog), WithContextExtensions by WithContextExtensions(context) {

    override val binder = FragmentEnableMultisigPushesWarningBinding.inflate(LayoutInflater.from(context))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder.enableMultisigPushesNotNow.setOnClickListener { dismiss() }
        binder.enableMultisigPushesEnable.setOnClickListener {
            onEnableClicked()
            dismiss()
        }
    }
}
