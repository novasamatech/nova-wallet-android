package io.novafoundation.nova.feature_multisig_operations.presentation.created

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.bottomSheet.action.fragment.ActionBottomSheetDialogFragment
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureApi
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureComponent

class MultisigCreatedBottomSheet : ActionBottomSheetDialogFragment<MultisigCreatedViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<MultisigOperationsFeatureComponent>(requireContext(), MultisigOperationsFeatureApi::class.java)
            .multisigCreated()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: MultisigCreatedViewModel) {}

    override fun tryToDismiss() {
        // Nothing to do. We handle dismissing in view model
    }
}
