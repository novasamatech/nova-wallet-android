package io.novafoundation.nova.feature_assets.presentation.novacard.waiting

import android.content.DialogInterface
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.FragmentWaitingNovaCardTopUpBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent

class WaitingNovaCardTopUpFragment : BaseBottomSheetFragment<WaitingNovaCardTopUpViewModel, FragmentWaitingNovaCardTopUpBinding>() {

    override fun createBinding() = FragmentWaitingNovaCardTopUpBinding.inflate(layoutInflater)

    override fun initViews() {
        dialog?.setCanceledOnTouchOutside(false)
        getBehaviour().isHideable = false
    }

    override fun onCancel(dialog: DialogInterface) {
        viewModel.closeNovaCard()

        super.onCancel(dialog)
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .waitingNovaCardTopUpComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: WaitingNovaCardTopUpViewModel) {
        viewModel.titleFlow.observe {
            binder.topUpWaitingTitle.text = it
        }

        binder.waitingTopUpCardTimer.startTimer(
            value = viewModel.getTimerValue(),
            customMessageFormat = R.string.waiting_top_up_card_timer,
            durationFormatter = viewModel.getTimerFormatter(),
            lifecycle = lifecycle,
            onFinish = { viewModel.timerFinished() }
        )
    }
}
