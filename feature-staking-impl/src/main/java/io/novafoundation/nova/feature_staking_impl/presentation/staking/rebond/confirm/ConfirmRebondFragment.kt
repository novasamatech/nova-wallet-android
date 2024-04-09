package io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_confirm_rebond.confirmRebondAmount
import kotlinx.android.synthetic.main.fragment_confirm_rebond.confirmRebondConfirm
import kotlinx.android.synthetic.main.fragment_confirm_rebond.confirmRebondExtrinsicInformation
import kotlinx.android.synthetic.main.fragment_confirm_rebond.confirmRebondHints
import kotlinx.android.synthetic.main.fragment_confirm_rebond.confirmRebondToolbar

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class ConfirmRebondFragment : BaseFragment<ConfirmRebondViewModel>() {

    companion object {

        fun getBundle(payload: ConfirmRebondPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_confirm_rebond, container, false)
    }

    override fun initViews() {
        confirmRebondToolbar.applyStatusBarInsets()

        confirmRebondExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }

        confirmRebondToolbar.setHomeButtonListener { viewModel.backClicked() }
        confirmRebondConfirm.prepareForProgress(viewLifecycleOwner)
        confirmRebondConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        val payload = argument<ConfirmRebondPayload>(PAYLOAD_KEY)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmRebondFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmRebondViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        observeHints(viewModel.hintsMixin, confirmRebondHints)
        setupFeeLoading(viewModel, confirmRebondExtrinsicInformation.fee)

        viewModel.showNextProgress.observe(confirmRebondConfirm::setProgressState)

        viewModel.amountModelFlow.observe(confirmRebondAmount::setAmount)

        viewModel.walletUiFlow.observe(confirmRebondExtrinsicInformation::setWallet)
        viewModel.feeLiveData.observe(confirmRebondExtrinsicInformation::setFeeStatus)
        viewModel.originAddressModelFlow.observe(confirmRebondExtrinsicInformation::setAccount)
    }
}
