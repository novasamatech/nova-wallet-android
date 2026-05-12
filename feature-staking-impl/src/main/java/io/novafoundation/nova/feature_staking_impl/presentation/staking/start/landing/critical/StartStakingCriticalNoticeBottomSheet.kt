package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.critical

import android.content.Context
import android.view.LayoutInflater
import io.novafoundation.nova.common.R as CommonR
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_staking_impl.data.notices.model.StakingNotice
import io.novafoundation.nova.feature_staking_impl.databinding.BottomSheetStartStakingCriticalNoticeBinding

class StartStakingCriticalNoticeBottomSheet(
    context: Context,
    private val notice: StakingNotice,
    private val onContinueClicked: () -> Unit,
) : BaseBottomSheet<BottomSheetStartStakingCriticalNoticeBinding>(
    context,
    CommonR.style.BottomSheetDialog,
) {

    override val binder: BottomSheetStartStakingCriticalNoticeBinding =
        BottomSheetStartStakingCriticalNoticeBinding.inflate(LayoutInflater.from(context))

    init {
        binder.startStakingCriticalNoticeTitle.text = notice.shortText
        binder.startStakingCriticalNoticeSubtitle.text = notice.longText

        binder.startStakingCriticalNoticeCancel.setOnClickListener { dismiss() }
        binder.startStakingCriticalNoticeConfirm.setOnClickListener {
            onContinueClicked()
            dismiss()
        }
    }
}
