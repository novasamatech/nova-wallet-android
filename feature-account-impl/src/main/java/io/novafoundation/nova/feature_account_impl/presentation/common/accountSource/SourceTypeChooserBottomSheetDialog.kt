package io.novafoundation.nova.feature_account_impl.presentation.common.accountSource

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.makeInvisible
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_account_impl.R
import kotlinx.android.synthetic.main.item_source.view.rightIcon
import kotlinx.android.synthetic.main.item_source.view.sourceTv

class SourceTypeChooserBottomSheetDialog<T : AccountSource>(
    context: Context,
    payload: Payload<T>,
    onClicked: ClickHandler<T>
) : DynamicListBottomSheet<T>(context, payload, AccountSourceDiffCallback(), onClicked) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.recovery_source_type)
    }

    override fun holderCreator(): HolderCreator<T> = {
        SourceTypeHolder(it.inflateChild(R.layout.item_source))
    }
}

class SourceTypeHolder<T : AccountSource>(itemView: View) : DynamicListSheetAdapter.Holder<T>(itemView) {

    override fun bind(item: T, isSelected: Boolean, handler: DynamicListSheetAdapter.Handler<T>) {
        super.bind(item, isSelected, handler)

        with(itemView) {
            if (isSelected) {
                rightIcon.makeVisible()
            } else {
                rightIcon.makeInvisible()
            }

            sourceTv.setText(item.nameRes)
        }
    }
}

private class AccountSourceDiffCallback<T : AccountSource> : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem.nameRes == newItem.nameRes
    }

    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
        return true
    }
}
