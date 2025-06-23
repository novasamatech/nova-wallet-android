package io.novafoundation.nova.feature_multisig_operations.presentation.details

import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_multisig_operations.databinding.FragmentMultisigOperationDetailsBinding
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureApi
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class MultisigOperationDetailsFragment : BaseFragment<MultisigOperationDetailsViewModel, FragmentMultisigOperationDetailsBinding>() {

    companion object {

        private const val PAYLOAD = "MultisigOperationDetailsFragment.Payload"

        fun getBundle(payload: MultisigOperationDetailsPayload) = bundleOf(PAYLOAD to payload)
    }

    override fun createBinding() = FragmentMultisigOperationDetailsBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.multisigPendingOperationDetailsContainer.applyStatusBarInsets()

        binder.multisigPendingOperationDetailsToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.multisigPendingOperationDetailsExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.multisigPendingOperationDetailsEnterCallData.setOnClickListener { viewModel.enterCallDataClicked() }
        binder.multisigPendingOperationDetailsAction.prepareForProgress(viewLifecycleOwner)
        binder.multisigPendingOperationDetailsAction.setOnClickListener { viewModel.actionClicked() }

        binder.multisigPendingOperationCallDetails.setOnClickListener { viewModel.callDetailsClicked() }
        binder.multisigPendingOperationCallDetails.background = with(requireContext()) {
            addRipple(getBlockDrawable())
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<MultisigOperationsFeatureComponent>(
            requireContext(),
            MultisigOperationsFeatureApi::class.java
        )
            .multisigOperationDetails()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: MultisigOperationDetailsViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel.feeLoaderMixin, binder.multisigPendingOperationDetailsExtrinsicInfo.fee)

        viewModel.showCallButtonState.observe(binder.multisigPendingOperationDetailsEnterCallData::isVisible::set)
        viewModel.actionButtonState.observe(binder.multisigPendingOperationDetailsAction::setState)
        viewModel.buttonAppearance.observe(binder.multisigPendingOperationDetailsAction::setAppearance)

        viewModel.currentAccountModelFlow.observe(binder.multisigPendingOperationDetailsExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(binder.multisigPendingOperationDetailsExtrinsicInfo::setWallet)

        viewModel.title.observe(binder.multisigPendingOperationDetailsToolbar::setTitle)

        viewModel.callDetailsVisible.observe(binder.multisigPendingOperationCallDetails::setVisible)
    }
}
