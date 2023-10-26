package io.novafoundation.nova.common.view.bottomSheet.list.fixed

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import kotlinx.android.synthetic.main.bottom_sheeet_fixed_list.fixedListSheetItemContainer
import kotlinx.android.synthetic.main.bottom_sheeet_fixed_list.fixedListSheetTitle
import kotlinx.android.synthetic.main.item_sheet_iconic_label.view.itemExternalActionContent
import kotlinx.android.synthetic.main.item_sheet_switcher.view.itemSheetSwitcher

typealias ViewGetter<V> = FixedListBottomSheet.() -> V

abstract class FixedListBottomSheet(
    context: Context,
    onCancel: (() -> Unit)? = null,
    private val viewConfiguration: ViewConfiguration = ViewConfiguration.default()
) : BaseBottomSheet(context, onCancel = onCancel) {

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

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

fun FixedListBottomSheet.textItem(
    @DrawableRes iconRes: Int,
    title: String,
    showArrow: Boolean = false,
    onClick: (View) -> Unit,
) {
    item(R.layout.item_sheet_iconic_label) { view ->
        view.itemExternalActionContent.text = title

        val paddingInDp = 12

        view.itemExternalActionContent.setDrawableStart(
            drawableRes = iconRes,
            widthInDp = 24,
            tint = R.color.icon_primary,
            paddingInDp = 12
        )

        if (showArrow) {
            view.itemExternalActionContent.setDrawableEnd(
                drawableRes = R.drawable.ic_chevron_right,
                widthInDp = 24,
                tint = R.color.icon_secondary,
                paddingInDp = paddingInDp
            )
        }

        view.setDismissingClickListener(onClick)
    }
}

fun FixedListBottomSheet.textItem(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    showArrow: Boolean = false,
    onClick: (View) -> Unit
) {
    textItem(iconRes, context.getString(titleRes), showArrow, onClick)
}

fun FixedListBottomSheet.switcherItem(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    initialState: Boolean,
    onClick: (View) -> Unit
) {
    item(R.layout.item_sheet_switcher) { view ->
        view.itemSheetSwitcher.setText(titleRes)
        view.itemSheetSwitcher.isChecked = initialState

        view.itemSheetSwitcher.setDrawableStart(
            drawableRes = iconRes,
            widthInDp = 24,
            tint = R.color.icon_primary,
            paddingInDp = 12
        )

        view.setDismissingClickListener(onClick)
    }
}
