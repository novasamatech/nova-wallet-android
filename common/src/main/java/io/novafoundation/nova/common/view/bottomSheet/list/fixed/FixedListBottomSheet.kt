package io.novafoundation.nova.common.view.bottomSheet.list.fixed

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.viewbinding.ViewBinding
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.BottomSheeetFixedListBinding
import io.novafoundation.nova.common.databinding.ItemSheetDescriptiveActionBinding
import io.novafoundation.nova.common.databinding.ItemSheetIconicLabelBinding
import io.novafoundation.nova.common.databinding.ItemSheetSwitcherBinding
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet

typealias ViewGetter<B, V> = FixedListBottomSheet.ViewConfiguration<B>.() -> V

abstract class FixedListBottomSheet<B : ViewBinding>(
    context: Context,
    onCancel: (() -> Unit)? = null,
    private val viewConfiguration: ViewConfiguration<B>
) : BaseBottomSheet<B>(context, onCancel = onCancel) {

    class ViewConfiguration<B : ViewBinding>(
        val configurationBinder: B,
        val container: ViewGetter<B, ViewGroup>,
        val title: ViewGetter<B, TextView>,
    ) {
        companion object {
            fun default(context: Context) = ViewConfiguration(
                configurationBinder = BottomSheeetFixedListBinding.inflate(LayoutInflater.from(context)),
                container = { configurationBinder.fixedListSheetItemContainer },
                title = { configurationBinder.fixedListSheetTitle },
            )
        }
    }

    override val binder: B = viewConfiguration.configurationBinder

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    final override fun setContentView(layoutResId: Int) {
        super.setContentView(layoutResId)
    }

    override fun setTitle(@StringRes titleRes: Int) {
        viewConfiguration.title(viewConfiguration).setText(titleRes)
    }

    override fun setTitle(title: CharSequence?) {
        viewConfiguration.title(viewConfiguration).setTextOrHide(title?.toString())
    }

    fun <IB : ViewBinding> item(binder: IB, builder: (IB) -> Unit) {
        val container = viewConfiguration.container(viewConfiguration)

        builder.invoke(binder)

        container.addView(binder.root)
    }

    fun addItem(view: View) {
        val container = viewConfiguration.container(viewConfiguration)
        container.addView(view)
    }

    fun <T : View> item(view: T, builder: (T) -> Unit) {
        builder.invoke(view)

        viewConfiguration.container(viewConfiguration).addView(view)
    }

    fun getCommonPadding(): Int {
        return 16.dp(context)
    }
}

fun FixedListBottomSheet<*>.textItem(
    @DrawableRes iconRes: Int,
    title: String,
    showArrow: Boolean = false,
    applyIconTint: Boolean = true,
    onClick: (View) -> Unit,
) {
    item(ItemSheetIconicLabelBinding.inflate(LayoutInflater.from(context))) { itemBinder ->
        itemBinder.itemExternalActionContent.text = title

        val paddingInDp = 12

        itemBinder.itemExternalActionContent.setDrawableStart(
            drawableRes = iconRes,
            widthInDp = 24,
            tint = R.color.icon_primary.takeIf { applyIconTint },
            paddingInDp = 12
        )

        if (showArrow) {
            itemBinder.itemExternalActionContent.setDrawableEnd(
                drawableRes = R.drawable.ic_chevron_right,
                widthInDp = 24,
                tint = R.color.icon_secondary,
                paddingInDp = paddingInDp
            )
        }

        itemBinder.root.setDismissingClickListener(onClick)
    }
}

fun FixedListBottomSheet<*>.textWithDescriptionItem(
    title: String,
    description: String,
    @DrawableRes iconRes: Int,
    enabled: Boolean = true,
    showArrowWhenEnabled: Boolean = false,
    onClick: (View) -> Unit,
) {
    item(ItemSheetDescriptiveActionBinding.inflate(LayoutInflater.from(context))) { itemBinder ->
        itemBinder.itemSheetDescriptiveActionTitle.text = title
        itemBinder.itemSheetDescriptiveActionSubtitle.text = description

        itemBinder.root.isEnabled = enabled

        itemBinder.itemSheetDescriptiveActionIcon.setImageResource(iconRes)

        itemBinder.itemSheetDescriptiveActionArrow.setVisible(enabled && showArrowWhenEnabled)

        if (enabled) {
            itemBinder.root.setDismissingClickListener(onClick)
        }
    }
}

fun FixedListBottomSheet<*>.textWithDescriptionItem(
    @StringRes titleRes: Int,
    @StringRes descriptionRes: Int,
    @DrawableRes iconRes: Int,
    enabled: Boolean = true,
    showArrowWhenEnabled: Boolean = false,
    onClick: (View) -> Unit,
) {
    textWithDescriptionItem(
        title = context.getString(titleRes),
        description = context.getString(descriptionRes),
        iconRes = iconRes,
        enabled = enabled,
        showArrowWhenEnabled = showArrowWhenEnabled,
        onClick = onClick
    )
}

fun FixedListBottomSheet<*>.textItem(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    showArrow: Boolean = false,
    applyIconTint: Boolean = true,
    onClick: (View) -> Unit
) {
    textItem(
        iconRes = iconRes,
        title = context.getString(titleRes),
        showArrow = showArrow,
        applyIconTint = applyIconTint,
        onClick = onClick
    )
}

fun FixedListBottomSheet<*>.switcherItem(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    initialState: Boolean,
    onClick: (View) -> Unit
) {
    item(ItemSheetSwitcherBinding.inflate(LayoutInflater.from(context))) { itemBinder ->
        itemBinder.itemSheetSwitcher.setText(titleRes)
        itemBinder.itemSheetSwitcher.isChecked = initialState

        itemBinder.itemSheetSwitcher.setDrawableStart(
            drawableRes = iconRes,
            widthInDp = 24,
            tint = R.color.icon_primary,
            paddingInDp = 12
        )

        itemBinder.root.setDismissingClickListener(onClick)
    }
}
