package io.novafoundation.nova.common.view.bottomSheet.list.dynamic

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.DialogExtensions
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setVisible
import kotlinx.android.synthetic.main.bottom_sheet_dynamic_list.dynamicListSheetContent
import kotlinx.android.synthetic.main.bottom_sheet_dynamic_list.dynamicListSheetHeader
import kotlinx.android.synthetic.main.bottom_sheet_dynamic_list.dynamicListSheetItemContainer
import kotlinx.android.synthetic.main.bottom_sheet_dynamic_list.dynamicListSheetRightAction
import kotlinx.android.synthetic.main.bottom_sheet_dynamic_list.dynamicListSheetTitle

typealias ClickHandler<T> = (T) -> Unit

class ReferentialEqualityDiffCallBack<T> : DiffUtil.ItemCallback<T>() {

    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
        return true
    }
}

abstract class BaseDynamicListBottomSheet(context: Context) :
    BottomSheetDialog(context, R.style.BottomSheetDialog),
    WithContextExtensions by WithContextExtensions(context),
    DialogExtensions {

    override val dialogInterface: DialogInterface
        get() = this

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

    final override fun setTitle(titleId: Int) {
        dynamicListSheetTitle.setText(titleId)
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
    private val onClicked: ClickHandler<T>,
    private val onCancel: (() -> Unit)? = null,
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
        onClicked(item)

        dismiss()
    }
}
