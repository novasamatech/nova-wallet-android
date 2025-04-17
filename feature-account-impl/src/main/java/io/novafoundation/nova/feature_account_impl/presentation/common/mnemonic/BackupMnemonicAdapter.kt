package io.novafoundation.nova.feature_account_impl.presentation.common.mnemonic

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_impl.databinding.ItemBackupMnemonicWordBinding
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.MnemonicWord
import kotlinx.android.extensions.LayoutContainer

class BackupMnemonicAdapter(
    private val itemHandler: ItemHandler
) : ListAdapter<MnemonicWord, BackupMnemonicAdapter.ConfirmMnemonicAdapterHolder>(DiffCallback) {

    fun interface ItemHandler {

        fun wordClicked(word: MnemonicWord)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfirmMnemonicAdapterHolder {
        return ConfirmMnemonicAdapterHolder(
            ItemBackupMnemonicWordBinding.inflate(parent.inflater(), parent, false),
            itemHandler
        )
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
        private val binder: ItemBackupMnemonicWordBinding,
        private val itemHandler: ItemHandler,
    ) : RecyclerView.ViewHolder(binder.root), LayoutContainer {

        override val containerView: View = binder.root

        fun bind(item: MnemonicWord) = with(containerView) {
            binder.itemConfirmMnemonicWord.text = item.content

            bindIndex(item)
            bindState(item)
        }

        fun bindState(item: MnemonicWord) = with(containerView) {
            val hasWord = !item.removed

            setVisible(hasWord, falseState = View.INVISIBLE)

            binder.itemConfirmMnemonicWord.setVisible(hasWord, falseState = View.INVISIBLE)

            if (item.removed) {
                setOnClickListener(null)
            } else {
                setOnClickListener { itemHandler.wordClicked(item) }
            }
        }

        fun bindIndex(item: MnemonicWord) {
            binder.itemConfirmMnemonicIndex.setTextOrHide(item.indexDisplay)
            binder.itemConfirmMnemonicWord.gravity = if (item.indexDisplay == null) Gravity.CENTER else Gravity.START
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
