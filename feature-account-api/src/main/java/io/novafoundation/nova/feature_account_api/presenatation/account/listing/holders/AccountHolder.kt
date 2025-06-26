package io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders

import android.animation.LayoutTransition
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.view.isVisible
import coil.ImageLoader
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.AlphaColorFilter
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.removeDrawableEnd
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.databinding.ItemAccountBinding
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi

class AccountHolder(
    private val binder: ItemAccountBinding,
    private val imageLoader: ImageLoader,
    @ColorRes private val chainBorderColor: Int
) : GroupedListHolder(binder.root) {

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

        binder.itemAccountContainer.layoutTransition = lt
        binder.itemChainIcon.backgroundTintList = containerView.context.getColorStateList(chainBorderColor)
    }

    fun bind(
        mode: Mode,
        accountModel: AccountUi,
        handler: AccountItemHandler?,
    ) = with(binder) {
        bindName(accountModel)
        bindSubtitle(accountModel)
        bindMode(mode, accountModel, handler)

        itemAccountIcon.setImageDrawable(accountModel.picture)
        itemChainIcon.letOrHide(accountModel.chainIcon) {
            itemChainIcon.colorFilter = AlphaColorFilter(accountModel.chainIconOpacity)
            itemChainIcon.setIcon(it, imageLoader = imageLoader)
        }

        if (accountModel.updateIndicator) {
            itemAccountTitle.setDrawableEnd(R.drawable.shape_account_updated_indicator, paddingInDp = 8)
        } else {
            itemAccountTitle.removeDrawableEnd()
        }
    }

    fun bindName(accountModel: AccountUi) {
        binder.itemAccountTitle.text = accountModel.title
    }

    fun bindSubtitle(accountModel: AccountUi) {
        binder.itemAccountSubtitle.setTextOrHide(accountModel.subtitle)
        binder.itemAccountSubtitle.setDrawableStart(accountModel.subtitleIconRes, paddingInDp = 4)
    }

    fun bindMode(
        mode: Mode,
        accountModel: AccountUi,
        handler: AccountItemHandler?,
    ) = with(binder) {
        when (mode) {
            Mode.VIEW -> {
                itemAccountArrow.visibility = View.GONE
                itemAccountDelete.visibility = View.GONE
                itemAccountRadioButton.visibility = View.GONE
                itemAccountCheckBox.visibility = View.GONE

                itemAccountDelete.setOnClickListener(null)

                root.setOnClickListener(null)
            }

            Mode.SELECT_MULTIPLE -> {
                itemAccountArrow.visibility = View.GONE

                itemAccountDelete.visibility = View.GONE

                itemAccountCheckBox.isVisible = accountModel.isClickable
                itemAccountCheckBox.isChecked = accountModel.isSelected

                itemAccountRadioButton.visibility = View.GONE

                root.setOnClickListener { handler?.itemClicked(accountModel) }
            }

            Mode.SELECT -> {
                itemAccountArrow.visibility = View.VISIBLE

                itemAccountDelete.visibility = View.GONE
                itemAccountDelete.setOnClickListener(null)

                itemAccountRadioButton.visibility = View.GONE
                itemAccountCheckBox.visibility = View.GONE

                root.setOnClickListener { handler?.itemClicked(accountModel) }
            }

            Mode.EDIT -> {
                itemAccountArrow.visibility = View.INVISIBLE

                itemAccountDelete.isVisible = accountModel.isEditable

                itemAccountDelete.setOnClickListener { handler?.deleteClicked(accountModel) }
                itemAccountDelete.setImageResource(R.drawable.ic_delete_symbol)

                itemAccountRadioButton.visibility = View.GONE
                itemAccountCheckBox.visibility = View.GONE

                root.setOnClickListener(null)
            }

            Mode.SWITCH -> {
                itemAccountArrow.visibility = View.GONE

                itemAccountDelete.visibility = View.GONE

                itemAccountRadioButton.isVisible = accountModel.isClickable
                itemAccountRadioButton.isChecked = accountModel.isSelected

                itemAccountCheckBox.visibility = View.GONE

                root.setOnClickListener { handler?.itemClicked(accountModel) }
            }
        }
    }
}
