package io.novafoundation.nova.feature_dapp_impl.presentation.search

import android.content.Context
import android.view.LayoutInflater
import io.novafoundation.nova.common.databinding.BottomSheetActionBinding
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetPayload
import io.novafoundation.nova.common.view.bottomSheet.action.ButtonPreferences
import io.novafoundation.nova.common.view.bottomSheet.action.TextAction
import io.novafoundation.nova.common.view.bottomSheet.action.primary
import io.novafoundation.nova.common.view.bottomSheet.action.setupView
import io.novafoundation.nova.feature_dapp_impl.R

class DAppStakingNoticeBottomSheet(
    context: Context,
    private val onGoToStaking: () -> Unit,
    private val onContinueToSite: () -> Unit,
) : BaseBottomSheet<BottomSheetActionBinding>(context) {

    override val binder: BottomSheetActionBinding = BottomSheetActionBinding.inflate(LayoutInflater.from(context))

    private var continueRevealed = false

    init {
        bindContent()
    }

    private fun bindContent() {
        binder.setupView(
            payload = ActionBottomSheetPayload(
                imageRes = R.drawable.ic_info_24,
                title = context.getString(R.string.dapp_staking_notice_title),
                subtitle = context.getString(R.string.dapp_staking_notice_message),
                actionButtonPreferences = ButtonPreferences.primary(context.getString(R.string.common_go_to_staking)) {
                    dismiss()
                    onGoToStaking()
                },
                secondaryTextAction = TextAction(secondaryText(), ::onSecondaryClicked)
            ),
            onPositiveButtonClicked = null,
            onNeutralButtonClicked = null
        )
    }

    private fun onSecondaryClicked() {
        if (continueRevealed) {
            dismiss()
            onContinueToSite()
        } else {
            continueRevealed = true
            bindContent()
        }
    }

    private fun secondaryText(): String {
        val textRes = if (continueRevealed) R.string.dapp_staking_notice_continue else R.string.common_advanced

        return context.getString(textRes)
    }
}
