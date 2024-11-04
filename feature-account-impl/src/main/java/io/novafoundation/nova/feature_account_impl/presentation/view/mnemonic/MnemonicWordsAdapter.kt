package io.novafoundation.nova.feature_account_impl.presentation.view.mnemonic

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_account_impl.databinding.ItemMnemonicWordBinding

class MnemonicWordsAdapter : ListAdapter<MnemonicWordModel, MnemonicWordViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MnemonicWordViewHolder {
        val binder = ItemMnemonicWordBinding.inflate(parent.inflater(), parent, false)
        return MnemonicWordViewHolder(binder)
    }

    override fun onBindViewHolder(holder: MnemonicWordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

object DiffCallback : DiffUtil.ItemCallback<MnemonicWordModel>() {

    override fun areItemsTheSame(oldItem: MnemonicWordModel, newItem: MnemonicWordModel): Boolean {
        return oldItem.numberToShow == newItem.numberToShow
    }

    override fun areContentsTheSame(oldItem: MnemonicWordModel, newItem: MnemonicWordModel): Boolean {
        return oldItem.word == newItem.word
    }
}

class MnemonicWordViewHolder(private val binder: ItemMnemonicWordBinding) : RecyclerView.ViewHolder(binder.root) {

    fun bind(mnemonicWord: MnemonicWordModel) {
        with(binder) {
            numberTv.text = mnemonicWord.numberToShow
            wordTv.text = mnemonicWord.word
        }
    }
}
