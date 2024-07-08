package io.novafoundation.nova.feature_account_impl.presentation.node.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.core.model.Node
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.node.list.accounts.AccountChooserBottomSheetDialog
import io.novafoundation.nova.feature_account_impl.presentation.node.model.NodeModel
import kotlinx.android.synthetic.main.fragment_nodes.addConnectionTv
import kotlinx.android.synthetic.main.fragment_nodes.connectionsList
import kotlinx.android.synthetic.main.fragment_nodes.novaToolbar

class NodesFragment : BaseFragment<NodesViewModel>(), NodesAdapter.NodeItemHandler {

    private lateinit var adapter: NodesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_nodes, container, false)

    override fun initViews() {
        adapter = NodesAdapter(this)

        connectionsList.setHasFixedSize(true)
        connectionsList.adapter = adapter

        novaToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }

        novaToolbar.setRightActionClickListener {
            viewModel.editClicked()
        }

        addConnectionTv.setOnClickListener {
            viewModel.addNodeClicked()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .connectionsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: NodesViewModel) {
        viewModel.groupedNodeModelsLiveData.observe(adapter::submitList)

        viewModel.noAccountsEvent.observeEvent {
            showNoAccountsDialog(it)
        }

        viewModel.showAccountChooserLiveData.observeEvent {
            AccountChooserBottomSheetDialog(requireActivity(), it) { _, item ->
                viewModel.accountSelected(item)
            }.show()
        }

        viewModel.editMode.observe(adapter::switchToEdit)

        viewModel.toolbarAction.observe(novaToolbar::setTextRight)

        viewModel.deleteNodeEvent.observeEvent(::showDeleteNodeDialog)
    }

    override fun infoClicked(nodeModel: NodeModel) {
        viewModel.infoClicked(nodeModel)
    }

    override fun checkClicked(nodeModel: NodeModel) {
        viewModel.selectNodeClicked(nodeModel)
    }

    override fun deleteClicked(nodeModel: NodeModel) {
        viewModel.deleteNodeClicked(nodeModel)
    }

    private fun showDeleteNodeDialog(nodeModel: NodeModel) {
        val message = getString(
            R.string.connection_delete_description,
            nodeModel.networkModelType.networkType.readableName,
            nodeModel.name
        )

        MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme)
            .setTitle(R.string.connection_delete_title)
            .setMessage(message)
            .setPositiveButton(R.string.common_delete) { dialog, _ ->
                viewModel.confirmNodeDeletion(nodeModel)
                dialog?.dismiss()
            }
            .setNegativeButton(R.string.common_cancel) { dialog, _ -> dialog?.dismiss() }
            .show()
    }

    private fun showNoAccountsDialog(networkType: Node.NetworkType) {
        MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme)
            .setTitle(R.string.account_needed_title)
            .setMessage(R.string.account_needed_message)
            .setPositiveButton(R.string.common_proceed) { dialog, _ ->
                viewModel.createAccountForNetworkType(networkType)
                dialog?.dismiss()
            }
            .setNegativeButton(R.string.common_cancel) { dialog, _ -> dialog?.dismiss() }
            .show()
    }
}
