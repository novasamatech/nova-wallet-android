package io.novafoundation.nova.feature_account_api.presenatation.account.listing

import android.animation.LayoutTransition
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.isVisible
import coil.ImageLoader
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.view.ChipLabelModel
import io.novafoundation.nova.common.view.ChipLabelView
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import kotlinx.android.synthetic.main.item_account.view.itemAccountArrow
import kotlinx.android.synthetic.main.item_account.view.itemAccountCheck
import kotlinx.android.synthetic.main.item_account.view.itemAccountContainer
import kotlinx.android.synthetic.main.item_account.view.itemAccountDelete
import kotlinx.android.synthetic.main.item_account.view.itemAccountIcon
import kotlinx.android.synthetic.main.item_account.view.itemAccountSubtitle
import kotlinx.android.synthetic.main.item_account.view.itemAccountTitle
import kotlinx.android.synthetic.main.item_account.view.itemChainIcon

class AccountsAdapter(
    private val accountItemHandler: AccountItemHandler,
    private val imageLoader: ImageLoader,
    initialMode: Mode
) : GroupedListAdapter<ChipLabelModel, AccountUi>(DiffCallback()) {

    private var mode: Mode = initialMode

    enum class Mode {
        VIEW, EDIT, SWITCH
    }

    interface AccountItemHandler {

        fun itemClicked(accountModel: AccountUi)

        fun deleteClicked(accountModel: AccountUi) {
            // default no op
        }
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
        return AccountHolder(parent.inflateChild(R.layout.item_account), imageLoader)
    }

    override fun bindGroup(holder: GroupedListHolder, group: ChipLabelModel) {
        (holder as AccountTypeHolder).bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: AccountUi) {
        (holder as AccountHolder).bind(mode, child, accountItemHandler)
    }

    override fun bindChild(holder: GroupedListHolder, position: Int, child: AccountUi, payloads: List<Any>) {
        require(holder is AccountHolder)

        resolvePayload(
            holder,
            position,
            payloads,
            onUnknownPayload = { holder.bindMode(mode, child, accountItemHandler) },
            onDiffCheck = {
                when (it) {
                    AccountUi::title -> holder.bindName(child)
                    AccountUi::subtitle -> holder.bindSubtitle(child)
                    AccountUi::isSelected -> holder.bindMode(mode, child, accountItemHandler)
                }
            }
        )
    }
}

class AccountTypeHolder(override val containerView: ChipLabelView) : GroupedListHolder(containerView) {

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

class AccountHolder(view: View, private val imageLoader: ImageLoader) : GroupedListHolder(view) {

    init {
        val lt = LayoutTransition().apply {
            disableTransitionType(LayoutTransition.DISAPPEARING)
            disableTransitionType(LayoutTransition.APPEARING)
        }

        containerView.itemAccountContainer.layoutTransition = lt
    }

    fun bind(
        mode: AccountsAdapter.Mode,
        accountModel: AccountUi,
        handler: AccountsAdapter.AccountItemHandler,
    ) = with(containerView) {
        bindName(accountModel)
        bindSubtitle(accountModel)
        bindMode(mode, accountModel, handler)

        itemAccountIcon.setImageDrawable(accountModel.picture)
        itemChainIcon.letOrHide(accountModel.chainIconUrl) {
            itemChainIcon.loadChainIcon(it, imageLoader = imageLoader)
        }
    }

    fun bindName(accountModel: AccountUi) {
        containerView.itemAccountTitle.text = accountModel.title
    }

    fun bindSubtitle(accountModel: AccountUi) {
        containerView.itemAccountSubtitle.text = accountModel.subtitle
        containerView.itemAccountSubtitle.setDrawableStart(accountModel.subtitleIconRes, paddingInDp = 4)
    }

    fun bindMode(
        mode: AccountsAdapter.Mode,
        accountModel: AccountUi,
        handler: AccountsAdapter.AccountItemHandler,
    ) = with(containerView) {
        when (mode) {
            AccountsAdapter.Mode.VIEW -> {
                itemAccountArrow.visibility = View.VISIBLE

                itemAccountDelete.visibility = View.GONE
                itemAccountDelete.setOnClickListener(null)

                itemAccountCheck.visibility = View.GONE

                setOnClickListener { handler.itemClicked(accountModel) }
            }

            AccountsAdapter.Mode.EDIT -> {
                itemAccountArrow.visibility = View.INVISIBLE

                itemAccountDelete.visibility = View.VISIBLE
                itemAccountDelete.setOnClickListener { handler.deleteClicked(accountModel) }
                itemAccountDelete.setImageResource(R.drawable.ic_delete_symbol)

                itemAccountCheck.visibility = View.GONE

                setOnClickListener(null)
            }

            AccountsAdapter.Mode.SWITCH -> {
                itemAccountArrow.visibility = View.GONE

                itemAccountDelete.visibility = View.GONE

                itemAccountCheck.isVisible = accountModel.isClickable

                itemAccountCheck.isChecked = accountModel.isSelected

                setOnClickListener { handler.itemClicked(accountModel) }
            }
        }
    }
}

private object MetaAccountPayloadGenerator : PayloadGenerator<AccountUi>(
    AccountUi::title,
    AccountUi::subtitle,
    AccountUi::isSelected
)

private class DiffCallback : BaseGroupedDiffCallback<ChipLabelModel, AccountUi>(ChipLabelModel::class.java) {
    override fun areGroupItemsTheSame(oldItem: ChipLabelModel, newItem: ChipLabelModel): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areGroupContentsTheSame(oldItem: ChipLabelModel, newItem: ChipLabelModel): Boolean {
        return oldItem.iconRes == newItem.iconRes
    }

    override fun areChildItemsTheSame(oldItem: AccountUi, newItem: AccountUi): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areChildContentsTheSame(oldItem: AccountUi, newItem: AccountUi): Boolean {
        return oldItem.title == newItem.title && oldItem.subtitle == newItem.subtitle && oldItem.isSelected == newItem.isSelected
    }

    override fun getChildChangePayload(oldItem: AccountUi, newItem: AccountUi): Any? {
        return MetaAccountPayloadGenerator.diff(oldItem, newItem)
    }
}
