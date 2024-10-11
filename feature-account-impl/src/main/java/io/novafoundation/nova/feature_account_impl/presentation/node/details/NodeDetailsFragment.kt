package io.novafoundation.nova.feature_account_impl.presentation.node.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.onTextChanged
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent

class NodeDetailsFragment : BaseFragment<NodeDetailsViewModel>() {

    companion object {
        private const val KEY_NODE_ID = "node_id"

        fun getBundle(nodeId: Int): Bundle {
            return Bundle().apply {
                putInt(KEY_NODE_ID, nodeId)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_node_details, container, false)

    override fun initViews() {
        novaToolbar.setHomeButtonListener { viewModel.backClicked() }

        nodeHostCopy.setOnClickListener {
            viewModel.copyNodeHostClicked()
        }

        updateBtn.setOnClickListener {
            viewModel.updateClicked(nodeDetailsNameField.content.text.toString(), nodeDetailsHostField.content.text.toString())
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
            nodeDetailsNameField.content.setText(node.name)
            nodeDetailsHostField.content.setText(node.link)

            with(node.networkModelType) {
                nodeDetailsNetworkType.text = networkType.readableName
                nodeDetailsNetworkType.setDrawableStart(icon)
            }
        }

        viewModel.nameEditEnabled.observe { editEnabled ->
            updateBtn.setVisible(editEnabled)

            nodeDetailsNameField.content.isEnabled = editEnabled

            nodeDetailsNameField.content.onTextChanged {
                viewModel.nodeDetailsEdited()
            }
        }

        viewModel.hostEditEnabled.observe { editEnabled ->
            nodeDetailsHostField.content.isEnabled = editEnabled

            nodeDetailsHostField.content.onTextChanged {
                viewModel.nodeDetailsEdited()
            }
        }

        viewModel.updateButtonEnabled.observe {
            updateBtn.isEnabled = it
        }
    }
}
