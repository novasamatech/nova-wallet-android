package io.novafoundation.nova.common.view.bottomSheet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.view.setPadding
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.BottomSheetActionNotAllowedBinding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.view.PrimaryButton

open class ActionNotAllowedBottomSheet(
    context: Context,
    private val onSuccess: () -> Unit,
) : BaseBottomSheet<BottomSheetActionNotAllowedBinding>(context, R.style.BottomSheetDialog), WithContextExtensions by WithContextExtensions(context) {

    override val binder = BottomSheetActionNotAllowedBinding.inflate(LayoutInflater.from(context))

    val iconView: ImageView
        get() = binder.actionNotAllowedImage

    val titleView: TextView
        get() = binder.actionNotAllowedTitle

    val subtitleView: TextView
        get() = binder.actionNotAllowedSubtitle

    val buttonView: PrimaryButton
        get() = binder.actionNotAllowedOk

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setOnDismissListener { onSuccess() }

        binder.actionNotAllowedOk.setOnClickListener {
            dismiss()
        }
    }

    // make it final so we will be able to call it from constructor without the risk of initialization conflict
    final override fun setContentView(layoutResId: Int) {
        super.setContentView(layoutResId)
    }

    protected fun applySolidIconStyle(@DrawableRes src: Int, @ColorRes tint: Int? = R.color.icon_primary) = with(iconView) {
        setPadding(12.dp)
        setImageTintRes(tint)
        setBackgroundResource(R.drawable.bg_icon_big)
        setImageResource(src)
    }

    protected fun applyDashedIconStyle(@DrawableRes src: Int, @ColorRes tint: Int? = R.color.icon_primary) = with(iconView) {
        setPadding(12.dp)
        setImageTintRes(tint)
        setBackgroundResource(R.drawable.bg_icon_big_dashed)
        setImageResource(src)
    }
}
