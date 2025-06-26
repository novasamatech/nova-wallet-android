package io.novafoundation.nova.feature_multisig_operations.presentation.enterCall

import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.applyImeInsetts
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_multisig_operations.databinding.FragmentMultisigOperationEnterCallBinding
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureApi
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureComponent

class MultisigOperationEnterCallFragment : BaseFragment<MultisigOperationEnterCallViewModel, FragmentMultisigOperationEnterCallBinding>() {

    companion object : PayloadCreator<MultisigOperationEnterCallPayload> by FragmentPayloadCreator()

    override fun createBinding() = FragmentMultisigOperationEnterCallBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<MultisigOperationsFeatureComponent>(
            requireContext(),
            MultisigOperationsFeatureApi::class.java
        )
            .multisigOperationEnterCall()
            .create(this, payload())
            .inject(this)
    }

    override fun initViews() {
        binder.multisigOperationEnterCallToolbar.applyStatusBarInsets()
        binder.root.applyImeInsetts()

        binder.multisigOperationEnterCallAction.setOnClickListener { viewModel.approve() }
    }

    override fun subscribe(viewModel: MultisigOperationEnterCallViewModel) {
        binder.multisigOperationEnterCallInput.content.bindTo(viewModel.enteredCall, viewLifecycleOwner.lifecycleScope)

        viewModel.buttonState.observe {
            binder.multisigOperationEnterCallAction.setState(it)
        }
    }
}
