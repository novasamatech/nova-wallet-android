package io.novafoundation.nova.feature_account_impl.presentation.node.details

import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.onTextChanged
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.databinding.FragmentNodeDetailsBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent

class NodeDetailsFragment : BaseFragment<NodeDetailsViewModel, FragmentNodeDetailsBinding>() {

    companion object {
        private const val KEY_NODE_ID = "node_id"

        fun getBundle(nodeId: Int): Bundle {
            return Bundle().apply {
                putInt(KEY_NODE_ID, nodeId)
            }
        }
    }

    override fun createBinding() = FragmentNodeDetailsBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.novaToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.nodeHostCopy.setOnClickListener {
            viewModel.copyNodeHostClicked()
        }

        binder.updateBtn.setOnClickListener {
            viewModel.updateClicked(binder.nodeDetailsNameField.content.text.toString(), binder.nodeDetailsHostField.content.text.toString())
        }
    }

    override fun inject() {
        val nodeId = argument<Int>(KEY_NODE_ID)
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .nodeDetailsComponentFactory()
            .create(this, nodeId)
            .inject(this)
    }

    override fun subscribe(viewModel: NodeDetailsViewModel) {
        viewModel.nodeModelLiveData.observe { node ->
            binder.nodeDetailsNameField.content.setText(node.name)
            binder.nodeDetailsHostField.content.setText(node.link)

            with(node.networkModelType) {
                binder.nodeDetailsNetworkType.text = networkType.readableName
                binder.nodeDetailsNetworkType.setDrawableStart(icon)
            }
        }

        viewModel.nameEditEnabled.observe { editEnabled ->
            binder.updateBtn.setVisible(editEnabled)

            binder.nodeDetailsNameField.content.isEnabled = editEnabled

            binder.nodeDetailsNameField.content.onTextChanged {
                viewModel.nodeDetailsEdited()
            }
        }

        viewModel.hostEditEnabled.observe { editEnabled ->
            binder.nodeDetailsHostField.content.isEnabled = editEnabled

            binder.nodeDetailsHostField.content.onTextChanged {
                viewModel.nodeDetailsEdited()
            }
        }

        viewModel.updateButtonEnabled.observe {
            binder.updateBtn.isEnabled = it
        }
    }
}
