package io.novafoundation.nova.feature_account_impl.presentation.common.mnemonic

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.MnemonicWord
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_backup_mnemonic_word.view.itemConfirmMnemonicIndex
import kotlinx.android.synthetic.main.item_backup_mnemonic_word.view.itemConfirmMnemonicWord

class BackupMnemonicAdapter(
    private val itemHandler: ItemHandler
) : ListAdapter<MnemonicWord, BackupMnemonicAdapter.ConfirmMnemonicAdapterHolder>(DiffCallback) {

    fun interface ItemHandler {

        fun wordClicked(word: MnemonicWord)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfirmMnemonicAdapterHolder {
        return ConfirmMnemonicAdapterHolder(parent.inflateChild(R.layout.item_backup_mnemonic_word), itemHandler)
    }

    override fun onBindViewHolder(holder: ConfirmMnemonicAdapterHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ConfirmMnemonicAdapterHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)

        resolvePayload(holder, position, payloads) {
            when (it) {
                MnemonicWord::removed -> holder.bindState(item)
                MnemonicWord::indexDisplay -> holder.bindIndex(item)
            }
        }
    }

    class ConfirmMnemonicAdapterHolder(
        override val containerView: View,
        private val itemHandler: ItemHandler,
    ) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(item: MnemonicWord) = with(containerView) {
            itemConfirmMnemonicWord.text = item.content

            bindIndex(item)
            bindState(item)
        }

        fun bindState(item: MnemonicWord) = with(containerView) {
            val hasWord = !item.removed

            isEnabled = hasWord

            itemConfirmMnemonicWord.setVisible(hasWord, falseState = View.INVISIBLE)

            if (item.removed) {
                setOnClickListener(null)
            } else {
                setOnClickListener { itemHandler.wordClicked(item) }
            }
        }

        fun bindIndex(item: MnemonicWord) {
            containerView.itemConfirmMnemonicIndex.setTextOrHide(item.indexDisplay)
        }
    }

    private object MnemonicPayloadGenerator : PayloadGenerator<MnemonicWord>(MnemonicWord::removed, MnemonicWord::indexDisplay)

    private object DiffCallback : DiffUtil.ItemCallback<MnemonicWord>() {

        override fun areItemsTheSame(oldItem: MnemonicWord, newItem: MnemonicWord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MnemonicWord, newItem: MnemonicWord): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: MnemonicWord, newItem: MnemonicWord): Any? {
            return MnemonicPayloadGenerator.diff(oldItem, newItem)
        }
    }
}
