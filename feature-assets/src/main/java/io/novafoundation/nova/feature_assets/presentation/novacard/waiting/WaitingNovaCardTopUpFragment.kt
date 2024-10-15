package io.novafoundation.nova.feature_assets.presentation.novacard.waiting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import kotlinx.android.synthetic.main.fragment_waiting_nova_card_top_up.waitingTopUpCardTimer

class WaitingNovaCardTopUpFragment : BaseBottomSheetFragment<WaitingNovaCardTopUpViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_waiting_nova_card_top_up, container, false)
    }

    override fun initViews() {
        dialog?.setCanceledOnTouchOutside(false)
        isCancelable = false
        getBehaviour().isHideable = false
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .waitingNovaCardTopUpComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: WaitingNovaCardTopUpViewModel) {
        waitingTopUpCardTimer.startTimer(
            viewModel.getTimerValue(),
            customMessageFormat = R.string.waiting_top_up_card_timer,
            timerDurationFormatter = viewModel.getTimerFormatter(),
            lifecycle = lifecycle,
            onFinish = { viewModel.timerFinished() }
        )
    }
}
