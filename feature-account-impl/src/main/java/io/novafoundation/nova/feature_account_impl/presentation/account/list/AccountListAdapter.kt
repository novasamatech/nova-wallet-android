package io.novafoundation.nova.feature_account_impl.presentation.account.list

import android.animation.LayoutTransition
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.account.model.MetaAccountUi
import kotlinx.android.synthetic.main.item_account.view.itemAccountArrow
import kotlinx.android.synthetic.main.item_account.view.itemAccountContainer
import kotlinx.android.synthetic.main.item_account.view.itemAccountDelete
import kotlinx.android.synthetic.main.item_account.view.itemAccountIcon
import kotlinx.android.synthetic.main.item_account.view.itemAccountSubtitle
import kotlinx.android.synthetic.main.item_account.view.itemAccountTitle


class AccountsAdapter(
    private val accountItemHandler: AccountItemHandler,
    initialMode: Mode
) : ListAdapter<MetaAccountUi, AccountHolder>(MetaAccountUiDiffCallback()) {

    private var mode: Mode = initialMode

    enum class Mode {
        VIEW, EDIT
    }

    interface AccountItemHandler {

        fun itemClicked(accountModel: MetaAccountUi)

        fun deleteClicked(accountModel: MetaAccountUi)
    }

    fun setMode(mode: Mode) {
        this.mode = mode

        notifyItemRangeChanged(0, itemCount, mode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountHolder {
        return AccountHolder(parent.inflateChild(R.layout.item_account))
    }

    override fun onBindViewHolder(holder: AccountHolder, position: Int) {
        holder.bind(mode, getItem(position), accountItemHandler)
    }

    override fun onBindViewHolder(holder: AccountHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)

        resolvePayload(
            holder, position, payloads,
            onUnknownPayload = { holder.bindMode(mode, item, accountItemHandler) },
            onDiffCheck = {
                when (it) {
                    MetaAccountUi::name -> holder.bindName(item)
                    MetaAccountUi::totalBalance -> holder.bindTotalBalance(item)
                    MetaAccountUi::isSelected -> holder.bindMode(mode, item, accountItemHandler)
                }
            }
        )
    }
}

class AccountHolder(view: View) : GroupedListHolder(view) {

    init {
        val lt = LayoutTransition().apply {
            disableTransitionType(LayoutTransition.DISAPPEARING)
            disableTransitionType(LayoutTransition.APPEARING)
        }

        containerView.itemAccountContainer.layoutTransition = lt
    }

    fun bind(
        mode: AccountsAdapter.Mode,
        accountModel: MetaAccountUi,
        handler: AccountsAdapter.AccountItemHandler,
    ) = with(containerView) {
        bindName(accountModel)
        bindTotalBalance(accountModel)
        bindMode(mode, accountModel, handler)

        itemAccountIcon.setImageDrawable(accountModel.picture)
    }

    fun bindName(accountModel: MetaAccountUi) {
        containerView.itemAccountTitle.text = accountModel.name
    }

    fun bindTotalBalance(accountModel: MetaAccountUi) {
        containerView.itemAccountSubtitle.text = accountModel.totalBalance
    }

    fun bindMode(
        mode: AccountsAdapter.Mode,
        accountModel: MetaAccountUi,
        handler: AccountsAdapter.AccountItemHandler,
    ) = with(containerView) {
        when (mode) {
            AccountsAdapter.Mode.VIEW -> {
                itemAccountArrow.visibility = View.VISIBLE
                itemAccountDelete.visibility = View.GONE

                itemAccountDelete.setOnClickListener(null)
                setOnClickListener { handler.itemClicked(accountModel) }
            }
            AccountsAdapter.Mode.EDIT -> {
                itemAccountArrow.visibility = View.INVISIBLE
                itemAccountDelete.setVisible(!accountModel.isSelected, falseState = View.INVISIBLE)
                itemAccountDelete.setOnClickListener { handler.deleteClicked(accountModel) }
                setOnClickListener(null)
            }
        }
    }
}

private object MetaAccountPayloadGenerator : PayloadGenerator<MetaAccountUi>(
    MetaAccountUi::name, MetaAccountUi::totalBalance, MetaAccountUi::isSelected
)

private class MetaAccountUiDiffCallback : DiffUtil.ItemCallback<MetaAccountUi>() {

    override fun areItemsTheSame(oldItem: MetaAccountUi, newItem: MetaAccountUi): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MetaAccountUi, newItem: MetaAccountUi): Boolean {
        return oldItem.name == newItem.name && oldItem.totalBalance == newItem.totalBalance && oldItem.isSelected == newItem.isSelected
    }

    override fun getChangePayload(oldItem: MetaAccountUi, newItem: MetaAccountUi): Any? {
        return MetaAccountPayloadGenerator.diff(oldItem, newItem)
    }
}

