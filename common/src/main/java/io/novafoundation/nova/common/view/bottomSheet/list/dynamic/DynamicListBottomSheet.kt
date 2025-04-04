package io.novafoundation.nova.common.view.bottomSheet.list.dynamic

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.DialogExtensions
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import kotlinx.android.synthetic.main.bottom_sheet_dynamic_list.dynamicListSheetContent
import kotlinx.android.synthetic.main.bottom_sheet_dynamic_list.dynamicListSheetHeader
import kotlinx.android.synthetic.main.bottom_sheet_dynamic_list.dynamicListSheetItemContainer
import kotlinx.android.synthetic.main.bottom_sheet_dynamic_list.dynamicListSheetRightAction
import kotlinx.android.synthetic.main.bottom_sheet_dynamic_list.dynamicListSheetSubtitle
import kotlinx.android.synthetic.main.bottom_sheet_dynamic_list.dynamicListSheetTitle

typealias ClickHandler<T> = (BaseDynamicListBottomSheet, T) -> Unit

class ReferentialEqualityDiffCallBack<T : Any> : DiffUtil.ItemCallback<T>() {

    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
        return true
    }
}

abstract class BaseDynamicListBottomSheet(context: Context) :
    BaseBottomSheet(context, R.style.BottomSheetDialog),
    WithContextExtensions by WithContextExtensions(context),
    DialogExtensions {

    protected val container: LinearLayout
        get() = dynamicListSheetItemContainer

    protected val headerView: View
        get() = dynamicListSheetHeader

    protected val recyclerView: RecyclerView
        get() = dynamicListSheetContent

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.bottom_sheet_dynamic_list)
        super.onCreate(savedInstanceState)
    }

    final override fun setTitle(title: CharSequence?) {
        dynamicListSheetTitle.text = title
    }

    fun setSubtitle(subtitle: CharSequence?) {
        dynamicListSheetSubtitle.setTextOrHide(subtitle)
    }

    final override fun setTitle(titleId: Int) {
        dynamicListSheetTitle.setText(titleId)
    }

    fun hideTitle() {
        dynamicListSheetTitle.setVisible(false)
    }

    fun setupRightAction(
        @DrawableRes drawableRes: Int,
        onClickListener: View.OnClickListener
    ) {
        dynamicListSheetRightAction.setImageResource(drawableRes)
        dynamicListSheetRightAction.setVisible(true)
        dynamicListSheetRightAction.setOnClickListener(onClickListener)
    }
}

abstract class DynamicListBottomSheet<T>(
    context: Context,
    private val payload: Payload<T>,
    private val diffCallback: DiffUtil.ItemCallback<T>,
    private val onClicked: ClickHandler<T>?,
    private val onCancel: (() -> Unit)? = null,
    private val dismissOnClick: Boolean = true
) : BaseDynamicListBottomSheet(context), DynamicListSheetAdapter.Handler<T> {

    open class Payload<out T>(val data: List<T>, val selected: T? = null)

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dynamicListSheetContent.setHasFixedSize(true)

        val adapter = DynamicListSheetAdapter(payload.selected, this, diffCallback, holderCreator())
        dynamicListSheetContent.adapter = adapter

        adapter.submitList(payload.data)

        setOnCancelListener { onCancel?.invoke() }
    }

    abstract fun holderCreator(): HolderCreator<T>

    override fun itemClicked(item: T) {
        onClicked?.invoke(this, item)

        if (dismissOnClick) {
            dismiss()
        }
    }
}
