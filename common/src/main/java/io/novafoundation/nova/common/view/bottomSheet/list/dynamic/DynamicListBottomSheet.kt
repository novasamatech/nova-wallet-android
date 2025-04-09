package io.novafoundation.nova.common.view.bottomSheet.list.dynamic

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.BottomSheetDynamicListBinding
import io.novafoundation.nova.common.utils.DialogExtensions
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet

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
    BaseBottomSheet<BottomSheetDynamicListBinding>(context, R.style.BottomSheetDialog),
    WithContextExtensions by WithContextExtensions(context),
    DialogExtensions {

    override val binder: BottomSheetDynamicListBinding = BottomSheetDynamicListBinding.inflate(LayoutInflater.from(context))

    protected val container: LinearLayout
        get() = binder.dynamicListSheetItemContainer

    protected val headerView: View
        get() = binder.dynamicListSheetHeader

    protected val recyclerView: RecyclerView
        get() = binder.dynamicListSheetContent

    final override fun setTitle(title: CharSequence?) {
        binder.dynamicListSheetTitle.text = title
    }

    fun setSubtitle(subtitle: CharSequence?) {
        binder.dynamicListSheetSubtitle.setTextOrHide(subtitle)
    }

    final override fun setTitle(titleId: Int) {
        binder.dynamicListSheetTitle.setText(titleId)
    }

    fun hideTitle() {
        dynamicListSheetTitle.setVisible(false)
    }

    fun setupRightAction(
        @DrawableRes drawableRes: Int,
        onClickListener: View.OnClickListener
    ) {
        binder.dynamicListSheetRightAction.setImageResource(drawableRes)
        binder.dynamicListSheetRightAction.setVisible(true)
        binder.dynamicListSheetRightAction.setOnClickListener(onClickListener)
    }
}

abstract class DynamicListBottomSheet<T : Any>(
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

        binder.dynamicListSheetContent.setHasFixedSize(true)

        val adapter = DynamicListSheetAdapter(payload.selected, this, diffCallback, holderCreator())
        binder.dynamicListSheetContent.adapter = adapter

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
