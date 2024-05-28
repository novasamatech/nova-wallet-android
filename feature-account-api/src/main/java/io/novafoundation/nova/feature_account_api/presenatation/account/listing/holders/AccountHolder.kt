package io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders

import android.animation.LayoutTransition
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.view.isVisible
import coil.ImageLoader
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.AlphaColorFilter
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import kotlinx.android.synthetic.main.item_account.view.itemAccountArrow
import kotlinx.android.synthetic.main.item_account.view.itemAccountCheckBox
import kotlinx.android.synthetic.main.item_account.view.itemAccountRadioButton
import kotlinx.android.synthetic.main.item_account.view.itemAccountContainer
import kotlinx.android.synthetic.main.item_account.view.itemAccountDelete
import kotlinx.android.synthetic.main.item_account.view.itemAccountIcon
import kotlinx.android.synthetic.main.item_account.view.itemAccountSubtitle
import kotlinx.android.synthetic.main.item_account.view.itemAccountTitle
import kotlinx.android.synthetic.main.item_account.view.itemChainIcon

class AccountHolder(view: View, private val imageLoader: ImageLoader, @ColorRes private val chainBorderColor: Int) : GroupedListHolder(view) {

    interface AccountItemHandler {

        fun itemClicked(accountModel: AccountUi)

        fun deleteClicked(accountModel: AccountUi) {
            // default no op
        }
    }

    enum class Mode {
        VIEW, SELECT, SELECT_MULTIPLE, EDIT, SWITCH
    }

    init {
        val lt = LayoutTransition().apply {
            disableTransitionType(LayoutTransition.DISAPPEARING)
            disableTransitionType(LayoutTransition.APPEARING)
        }

        containerView.itemAccountContainer.layoutTransition = lt
        containerView.itemChainIcon.backgroundTintList = containerView.context.getColorStateList(chainBorderColor)
    }

    fun bind(
        mode: Mode,
        accountModel: AccountUi,
        handler: AccountItemHandler?,
    ) = with(containerView) {
        bindName(accountModel)
        bindSubtitle(accountModel)
        bindMode(mode, accountModel, handler)

        itemAccountIcon.setImageDrawable(accountModel.picture)
        itemChainIcon.letOrHide(accountModel.chainIconUrl) {
            itemChainIcon.colorFilter = AlphaColorFilter(accountModel.chainIconOpacity)
            itemChainIcon.loadChainIcon(it, imageLoader = imageLoader)
        }

        if (accountModel.updateIndicator) {
            itemAccountTitle.setDrawableEnd(R.drawable.shape_account_updated_indicator, paddingInDp = 8)
        } else {
            itemAccountTitle.setDrawableEnd(null)
        }
    }

    fun bindName(accountModel: AccountUi) {
        containerView.itemAccountTitle.text = accountModel.title
    }

    fun bindSubtitle(accountModel: AccountUi) {
        containerView.itemAccountSubtitle.setTextOrHide(accountModel.subtitle)
        containerView.itemAccountSubtitle.setDrawableStart(accountModel.subtitleIconRes, paddingInDp = 4)
    }

    fun bindMode(
        mode: Mode,
        accountModel: AccountUi,
        handler: AccountItemHandler?,
    ) = with(containerView) {
        when (mode) {
            Mode.VIEW -> {
                itemAccountArrow.visibility = View.GONE
                itemAccountDelete.visibility = View.GONE
                itemAccountRadioButton.visibility = View.GONE
                itemAccountCheckBox.visibility = View.GONE

                itemAccountDelete.setOnClickListener(null)

                setOnClickListener(null)
            }

            Mode.SELECT_MULTIPLE -> {
                itemAccountArrow.visibility = View.GONE

                itemAccountDelete.visibility = View.GONE

                itemAccountCheckBox.isVisible = accountModel.isClickable
                itemAccountCheckBox.isChecked = accountModel.isSelected

                itemAccountRadioButton.visibility = View.GONE

                setOnClickListener { handler?.itemClicked(accountModel) }
            }

            Mode.SELECT -> {
                itemAccountArrow.visibility = View.VISIBLE

                itemAccountDelete.visibility = View.GONE
                itemAccountDelete.setOnClickListener(null)

                itemAccountRadioButton.visibility = View.GONE
                itemAccountCheckBox.visibility = View.GONE

                setOnClickListener { handler?.itemClicked(accountModel) }
            }

            Mode.EDIT -> {
                itemAccountArrow.visibility = View.INVISIBLE

                itemAccountDelete.isVisible = accountModel.isEditable

                itemAccountDelete.setOnClickListener { handler?.deleteClicked(accountModel) }
                itemAccountDelete.setImageResource(R.drawable.ic_delete_symbol)

                itemAccountRadioButton.visibility = View.GONE
                itemAccountCheckBox.visibility = View.GONE

                setOnClickListener(null)
            }

            Mode.SWITCH -> {
                itemAccountArrow.visibility = View.GONE

                itemAccountDelete.visibility = View.GONE

                itemAccountRadioButton.isVisible = accountModel.isClickable
                itemAccountRadioButton.isChecked = accountModel.isSelected

                itemAccountCheckBox.visibility = View.GONE

                setOnClickListener { handler?.itemClicked(accountModel) }
            }
        }
    }
}
