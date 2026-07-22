package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.critical

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
        // Match iOS: lock the modal so back-button + outside-tap + swipe-down can't bypass
        // the 10s friction. Cancel button or completed-countdown Continue are the only exits.
        setCancelable(false)

        binder.startStakingCriticalNoticeTitle.text = notice.shortText
        binder.startStakingCriticalNoticeSubtitle.text = notice.longText

        binder.startStakingCriticalNoticeCancel.setOnClickListener { dismiss() }
        binder.startStakingCriticalNoticeConfirm.setOnClickListener {
            onContinueClicked()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        // Block swipe-down dismissal. Material 1.1.0 doesn't have setDraggable;
        // isHideable=false achieves the same effect (sheet can't be dragged below peek).
        @Suppress("UNCHECKED_CAST")
        (behavior as BottomSheetBehavior<View>).isHideable = false
    }
}
