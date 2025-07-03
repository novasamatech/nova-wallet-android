package io.novafoundation.nova.feature_multisig_operations.presentation.details.full

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.copy.setupCopyText
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.common.view.bottomSheet.description.observeDescription
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAccountWithLoading
import io.novafoundation.nova.feature_multisig_operations.databinding.FragmentMultisigOperationFullDetailsBinding
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureApi
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureComponent
import io.novafoundation.nova.feature_multisig_operations.presentation.details.common.MultisigOperationDetailsPayload
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class MultisigOperationFullDetailsFragment : BaseFragment<MultisigOperationFullDetailsViewModel, FragmentMultisigOperationFullDetailsBinding>() {

    companion object : PayloadCreator<MultisigOperationDetailsPayload> by FragmentPayloadCreator()

    override fun createBinding() = FragmentMultisigOperationFullDetailsBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.root.applyStatusBarInsets()

        binder.multisigPendingOperationFullDetailsToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.multisigPendingOperationDetailsDepositor.setOnClickListener { viewModel.onDepositorClicked() }
        binder.multisigPendingOperationDetailsDeposit.setOnClickListener { viewModel.depositClicked() }
        binder.multisigPendingOperationDetailsCallData.setOnClickListener { viewModel.callHashClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<MultisigOperationsFeatureComponent>(
            requireContext(),
            MultisigOperationsFeatureApi::class.java
        )
            .multisigOperationFullDetails()
            .create(this, payload())
            .inject(this)
    }

    override fun subscribe(viewModel: MultisigOperationFullDetailsViewModel) {
        observeDescription(viewModel)
        setupExternalActions(viewModel)
        setupCopyText(viewModel)

        viewModel.depositorAccountModel.observe { binder.multisigPendingOperationDetailsDepositor.showAccountWithLoading(it) }
        viewModel.depositAmount.observe { binder.multisigPendingOperationDetailsDeposit.showAmount(it) }
        viewModel.ellipsizedCallHash.observe { binder.multisigPendingOperationDetailsCallData.showValueOrHide(it, null) }
        viewModel.formattedCall.observe { binder.multisigPendingOperationDetailsCall.text = it }
    }
}
