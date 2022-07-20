package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.sign

import android.content.Context
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.novafoundation.nova.feature_account_impl.R
import kotlinx.android.synthetic.main.watch_only_sign_bottom_sheet.watchOnlySignButton

class WatchOnlySignBottomSheet(
    context: Context,
    private val onSuccess: () -> Unit
): BottomSheetDialog(context, R.style.BottomSheetDialog) {

    init {
        setContentView(R.layout.watch_only_sign_bottom_sheet)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setOnDismissListener { onSuccess() }

        watchOnlySignButton.setOnClickListener {
            dismiss()
        }
    }
}
