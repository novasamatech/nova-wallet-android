package io.novafoundation.nova.common.view.bottomSheet.list.fixed

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.DialogExtensions
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.dp
import kotlinx.android.synthetic.main.bottom_sheeet_fixed_list.fixedListSheetItemContainer
import kotlinx.android.synthetic.main.bottom_sheeet_fixed_list.fixedListSheetTitle
import kotlinx.android.synthetic.main.item_sheet_iconic_label.view.itemExternalActionContent

typealias ViewGetter<V> = FixedListBottomSheet.() -> V

abstract class FixedListBottomSheet(
    context: Context,
    private val onCancel: (() -> Unit)? = null,
    private val viewConfiguration: ViewConfiguration = ViewConfiguration.default()
) : BottomSheetDialog(context, R.style.BottomSheetDialog), DialogExtensions {

    class ViewConfiguration(
        @LayoutRes val layout: Int,
        val container: ViewGetter<ViewGroup>,
        val title: ViewGetter<TextView>,
    ) {
        companion object {
            fun default() = ViewConfiguration(
                layout = R.layout.bottom_sheeet_fixed_list,
                container = { fixedListSheetItemContainer },
                title = { fixedListSheetTitle },
            )
        }
    }

    init {
        setContentView(viewConfiguration.layout)
    }

    final override val dialogInterface: DialogInterface
        get() = this

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setOnCancelListener { onCancel?.invoke() }
    }

    final override fun setContentView(layoutResId: Int) {
        super.setContentView(layoutResId)
    }

    override fun setTitle(@StringRes titleRes: Int) {
        viewConfiguration.title(this).setText(titleRes)
    }

    override fun setTitle(title: CharSequence?) {
        viewConfiguration.title(this).setTextOrHide(title?.toString())
    }

    fun item(@LayoutRes layoutRes: Int, builder: (View) -> Unit) {
        val container = viewConfiguration.container(this)

        val view = container.inflateChild(layoutRes)

        builder.invoke(view)

        container.addView(view)
    }

    fun addItem(view: View) {
        val container = viewConfiguration.container(this)
        container.addView(view)
    }

    fun <T : View> item(view: T, builder: (T) -> Unit) {
        builder.invoke(view)

        viewConfiguration.container(this).addView(view)
    }

    fun getCommonPadding(): Int {
        return 16.dp(context)
    }
}

fun FixedListBottomSheet.item(
    @DrawableRes icon: Int,
    title: String,
    showArrow: Boolean = false,
    onClick: (View) -> Unit,
) {
    item(R.layout.item_sheet_iconic_label) { view ->
        view.itemExternalActionContent.text = title

        val paddingInDp = 12

        view.itemExternalActionContent.setDrawableStart(
            drawableRes = icon,
            widthInDp = 24,
            tint = R.color.white,
            paddingInDp = 12
        )

        if (showArrow) {
            view.itemExternalActionContent.setDrawableEnd(
                drawableRes = R.drawable.ic_chevron_right,
                widthInDp = 24,
                tint = R.color.white_48,
                paddingInDp = paddingInDp
            )
        }

        view.setDismissingClickListener(onClick)
    }
}

fun FixedListBottomSheet.item(
    @DrawableRes icon: Int,
    @StringRes titleRes: Int,
    showArrow: Boolean = false,
    onClick: (View) -> Unit
) {
    item(icon, context.getString(titleRes), showArrow, onClick)
}
