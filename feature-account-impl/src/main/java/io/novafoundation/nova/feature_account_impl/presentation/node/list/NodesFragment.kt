package io.novafoundation.nova.feature_account_impl.presentation.node.list

import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.core.model.Node
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.FragmentNodesBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.node.list.accounts.AccountChooserBottomSheetDialog
import io.novafoundation.nova.feature_account_impl.presentation.node.model.NodeModel

class NodesFragment : BaseFragment<NodesViewModel, FragmentNodesBinding>(), NodesAdapter.NodeItemHandler {

    override val binder by viewBinding(FragmentNodesBinding::bind)

    private lateinit var adapter: NodesAdapter

    override fun initViews() {
        adapter = NodesAdapter(this)

        binder.connectionsList.setHasFixedSize(true)
        binder.connectionsList.adapter = adapter

        binder.novaToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }

        binder.novaToolbar.setRightActionClickListener {
            viewModel.editClicked()
        }

        binder.addConnectionTv.setOnClickListener {
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

        viewModel.toolbarAction.observe(binder.novaToolbar::setTextRight)

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
