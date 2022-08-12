package io.novafoundation.nova.common.view.bottomSheet

import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.setPadding
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.view.PrimaryButton
import kotlinx.android.synthetic.main.bottom_sheet_action_not_allowed.actionNotAllowedImage
import kotlinx.android.synthetic.main.bottom_sheet_action_not_allowed.actionNotAllowedOk
import kotlinx.android.synthetic.main.bottom_sheet_action_not_allowed.actionNotAllowedSubtitle
import kotlinx.android.synthetic.main.bottom_sheet_action_not_allowed.actionNotAllowedTitle

open class ActionNotAllowedBottomSheet(
    context: Context,
    private val onSuccess: () -> Unit,
) : BottomSheetDialog(context, R.style.BottomSheetDialog), WithContextExtensions by WithContextExtensions(context) {

    val image: ImageView
        get() = actionNotAllowedImage

    val title: TextView
        get() = actionNotAllowedTitle

    val subtitle: TextView
        get() = actionNotAllowedSubtitle

    val button: PrimaryButton
        get() = actionNotAllowedOk

    init {
        setContentView(R.layout.bottom_sheet_action_not_allowed)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setOnDismissListener { onSuccess() }

        actionNotAllowedOk.setOnClickListener {
            dismiss()
        }
    }

    // make it final so we will be able to call it from constructor without the risk of initialization conflict
    final override fun setContentView(layoutResId: Int) {
        super.setContentView(layoutResId)
    }

    protected fun applySolidIconStyle(@DrawableRes src: Int) = with(image) {
        setPadding(12.dp)
        setBackgroundResource(R.drawable.bg_icon_big)
        setImageResource(src)
    }

    protected fun applyDashedIconStyle(@DrawableRes src: Int) = with(image) {
        setPadding(12.dp)
        setBackgroundResource(R.drawable.bg_icon_big_dashed)
        setImageResource(src)
    }
}
