package io.novafoundation.nova.feature_account_impl.presentation.view.mnemonic

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.ViewMnemonicBinding

class MnemonicViewer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val adapter = MnemonicWordsAdapter()

    private val binder = ViewMnemonicBinding.inflate(inflater(), this)

    init {
        View.inflate(context, R.layout.view_mnemonic, this)

        binder.mnemonicViewerList.adapter = adapter
    }

    fun submitList(list: List<MnemonicWordModel>) {
        val manager = binder.mnemonicViewerList.layoutManager as GridLayoutManager

        manager.spanCount = list.size / 2

        adapter.submitList(list)
    }
}
