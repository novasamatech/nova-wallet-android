package io.novafoundation.nova.feature_multisig_operations.presentation.created

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.common.view.bottomSheet.action.fragment.ActionBottomSheetDialogFragment
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureApi
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureComponent

class MultisigCreatedBottomSheet : ActionBottomSheetDialogFragment<MultisigCreatedViewModel>() {

    companion object : PayloadCreator<MultisigCreatedPayload> by FragmentPayloadCreator()

    override fun inject() {
        FeatureUtils.getFeature<MultisigOperationsFeatureComponent>(requireContext(), MultisigOperationsFeatureApi::class.java)
            .multisigCreated()
            .create(this, payload())
            .inject(this)
    }

    override fun subscribe(viewModel: MultisigCreatedViewModel) {}
}
