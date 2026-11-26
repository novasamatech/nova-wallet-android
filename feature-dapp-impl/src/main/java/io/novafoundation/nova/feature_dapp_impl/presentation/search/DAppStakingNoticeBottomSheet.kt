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
import io.novafoundation.nova.feature_dapp_impl.domain.search.DAppStakingKind

/**
 * The Bittensor notice names the built-in Bittensor staking and offers "Continue to site" at once;
 * the notice for other staking sites reveals it after "Advanced".
 */
class DAppStakingNoticeBottomSheet(
    context: Context,
    private val kind: DAppStakingKind
) : BaseBottomSheet<BottomSheetActionBinding>(context) {

    override val binder: BottomSheetActionBinding = BottomSheetActionBinding.inflate(LayoutInflater.from(context))

    private var continueRevealed = kind == DAppStakingKind.BITTENSOR

    var selectedAction: StakingNoticeAction? = null
        private set

    init {
        bindContent()
    }

    private fun bindContent() {
        binder.setupView(
            payload = ActionBottomSheetPayload(
                imageRes = R.drawable.ic_info_24,
                title = context.getString(R.string.dapp_staking_notice_title),
                subtitle = context.getString(messageRes()),
                actionButtonPreferences = ButtonPreferences.primary(context.getString(R.string.common_go_to_staking)) {
                    selectAction(StakingNoticeAction.GO_TO_STAKING)
                },
                secondaryTextAction = TextAction(secondaryText(), ::onSecondaryClicked)
            ),
            onPositiveButtonClicked = null,
            onNeutralButtonClicked = null
        )
    }

    private fun onSecondaryClicked() {
        if (continueRevealed) {
            selectAction(StakingNoticeAction.CONTINUE_TO_SITE)
        } else {
            continueRevealed = true
            bindContent()
        }
    }

    private fun selectAction(action: StakingNoticeAction) {
        selectedAction = action
        dismiss()
    }

    private fun messageRes(): Int {
        return when (kind) {
            DAppStakingKind.BITTENSOR -> R.string.dapp_staking_notice_bittensor_message
            DAppStakingKind.STAKING -> R.string.dapp_staking_notice_message
        }
    }

    private fun secondaryText(): String {
        val textRes = if (continueRevealed) R.string.dapp_staking_notice_continue else R.string.common_advanced

        return context.getString(textRes)
    }
}
