package io.novafoundation.nova.feature_multisig_operations.presentation.details.general.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import coil.clear
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_multisig_operations.databinding.ItemMultisigSignatoryAccountBinding

class SignatoriesAdapter(
    private val handler: Handler
) : ListAdapter<SignatoryRvItem, SignatoryViewHolder>(SignatoriesDiffCallback()) {

    fun interface Handler {
        fun onSignatoryClicked(signatory: SignatoryRvItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SignatoryViewHolder {
        return SignatoryViewHolder(
            ItemMultisigSignatoryAccountBinding.inflate(parent.inflater(), parent, false),
            handler
        )
    }

    override fun onBindViewHolder(holder: SignatoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class SignatoryViewHolder(
    private val binder: ItemMultisigSignatoryAccountBinding,
    private val itemHandler: SignatoriesAdapter.Handler,
) : BaseViewHolder(binder.root) {

    fun bind(item: SignatoryRvItem) = with(binder) {
        root.setOnClickListener { itemHandler.onSignatoryClicked(item) }
        itemSignatoryAccountIcon.setImageDrawable(item.accountModel.drawable())
        itemSignatoryAccountSelected.setVisible(item.isApproved, falseState = View.INVISIBLE)
        itemSignatoryAccountTitle.text = item.accountModel.nameOrAddress()
        itemSignatoryAccountSubtitle.setTextOrHide(item.subtitle)
    }

    override fun unbind() {
        with(binder) {
            itemSignatoryAccountIcon.clear()
        }
    }
}

class SignatoriesDiffCallback : DiffUtil.ItemCallback<SignatoryRvItem>() {

    override fun areItemsTheSame(oldItem: SignatoryRvItem, newItem: SignatoryRvItem): Boolean {
        return oldItem.accountModel == newItem.accountModel
    }

    override fun areContentsTheSame(oldItem: SignatoryRvItem, newItem: SignatoryRvItem): Boolean {
        return oldItem == newItem
    }
}
