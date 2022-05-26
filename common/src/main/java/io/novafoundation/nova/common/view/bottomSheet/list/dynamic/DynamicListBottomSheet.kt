package io.novafoundation.nova.common.view.bottomSheet.list.dynamic

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.DialogExtensions
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setVisible
import kotlinx.android.synthetic.main.bottom_sheet_dynamic_list.dynamicListSheetContent
import kotlinx.android.synthetic.main.bottom_sheet_dynamic_list.dynamicListSheetDivider
import kotlinx.android.synthetic.main.bottom_sheet_dynamic_list.dynamicListSheetItemContainer
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

abstract class DynamicListBottomSheet<T>(
    context: Context,
    private val payload: Payload<T>,
    private val diffCallback: DiffUtil.ItemCallback<T>,
    private val onClicked: ClickHandler<T>,
    private val onCancel: (() -> Unit)? = null,
) : BottomSheetDialog(context, R.style.BottomSheetDialog),
    DynamicListSheetAdapter.Handler<T>,
    WithContextExtensions by WithContextExtensions(context),
    DialogExtensions {

    override val dialogInterface: DialogInterface
        get() = this

    class Payload<T>(val data: List<T>, val selected: T? = null)

    protected val container: LinearLayout
        get() = dynamicListSheetItemContainer

    protected val titleView: TextView
        get() = dynamicListSheetTitle

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.bottom_sheet_dynamic_list)
        super.onCreate(savedInstanceState)

        dynamicListSheetContent.setHasFixedSize(true)

        val adapter = DynamicListSheetAdapter(payload.selected, this, diffCallback, holderCreator())
        dynamicListSheetContent.adapter = adapter

        adapter.submitList(payload.data)

        setOnCancelListener { onCancel?.invoke() }
    }

    abstract fun holderCreator(): HolderCreator<T>

    override fun setTitle(title: CharSequence?) {
        dynamicListSheetTitle.text = title
    }

    override fun setTitle(titleId: Int) {
        dynamicListSheetTitle.setText(titleId)
    }

    fun setDividerVisible(visible: Boolean) {
        dynamicListSheetDivider.setVisible(visible)
    }

    override fun itemClicked(item: T) {
        onClicked(item)

        dismiss()
    }
}
