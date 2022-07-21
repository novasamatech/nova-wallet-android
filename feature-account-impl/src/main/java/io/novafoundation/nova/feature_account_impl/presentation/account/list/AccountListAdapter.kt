package io.novafoundation.nova.feature_account_impl.presentation.account.list

import android.animation.LayoutTransition
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.ChipLabelModel
import io.novafoundation.nova.common.view.ChipLabelView
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
) : GroupedListAdapter<ChipLabelModel, MetaAccountUi>(DiffCallback()) {

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

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        val view = ChipLabelView(parent.context)

        return AccountTypeHolder(view)
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return AccountHolder(parent.inflateChild(R.layout.item_account))
    }

    override fun bindGroup(holder: GroupedListHolder, group: ChipLabelModel) {
        (holder as AccountTypeHolder).bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: MetaAccountUi) {
        (holder as AccountHolder).bind(mode, child, accountItemHandler)
    }

    override fun bindChild(holder: GroupedListHolder, position: Int, child: MetaAccountUi, payloads: List<Any>) {
        require(holder is AccountHolder)

        resolvePayload(
            holder, position, payloads,
            onUnknownPayload = { holder.bindMode(mode, child, accountItemHandler) },
            onDiffCheck = {
                when (it) {
                    MetaAccountUi::name -> holder.bindName(child)
                    MetaAccountUi::totalBalance -> holder.bindTotalBalance(child)
                    MetaAccountUi::isSelected -> holder.bindMode(mode, child, accountItemHandler)
                }
            }
        )
    }
}

class AccountTypeHolder(override val containerView: ChipLabelView): GroupedListHolder(containerView) {

    init {
        val context = containerView.context

        containerView.layoutParams = ViewGroup.MarginLayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            setMargins(16.dp(context), 16.dp(context), 0, 8.dp(context))
        }
    }

    fun bind(item: ChipLabelModel) {
        containerView.setModel(item)
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
                itemAccountDelete.visibility = View.VISIBLE

                if (accountModel.isSelected) {
                    itemAccountDelete.setImageResource(R.drawable.ic_checkmark)
                    itemAccountDelete.setOnClickListener(null)
                } else {
                    itemAccountDelete.setOnClickListener {  handler.deleteClicked(accountModel) }
                    itemAccountDelete.setImageResource(R.drawable.ic_delete_symbol)
                }

                setOnClickListener(null)
            }
        }
    }
}

private object MetaAccountPayloadGenerator : PayloadGenerator<MetaAccountUi>(
    MetaAccountUi::name, MetaAccountUi::totalBalance, MetaAccountUi::isSelected
)

private class DiffCallback : BaseGroupedDiffCallback<ChipLabelModel, MetaAccountUi>(ChipLabelModel::class.java) {
    override fun areGroupItemsTheSame(oldItem: ChipLabelModel, newItem: ChipLabelModel): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areGroupContentsTheSame(oldItem: ChipLabelModel, newItem: ChipLabelModel): Boolean {
        return oldItem.iconRes == newItem.iconRes
    }

    override fun areChildItemsTheSame(oldItem: MetaAccountUi, newItem: MetaAccountUi): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areChildContentsTheSame(oldItem: MetaAccountUi, newItem: MetaAccountUi): Boolean {
        return oldItem.name == newItem.name && oldItem.totalBalance == newItem.totalBalance && oldItem.isSelected == newItem.isSelected
    }

    override fun getChildChangePayload(oldItem: MetaAccountUi, newItem: MetaAccountUi): Any? {
        return MetaAccountPayloadGenerator.diff(oldItem, newItem)
    }
}

