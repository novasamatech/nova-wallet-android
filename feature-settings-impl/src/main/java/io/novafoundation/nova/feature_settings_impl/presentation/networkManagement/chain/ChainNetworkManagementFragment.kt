package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain

import android.os.Bundle
import androidx.recyclerview.widget.ConcatAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.ViewSpace
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.common.view.input.selector.setupListSelectorMixin
import io.novafoundation.nova.common.view.recyclerview.adapter.text.TextAdapter
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.databinding.FragmentChainNetworkManagementBinding
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.headerAdapter.ChainNetworkManagementHeaderAdapter
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.ChainNetworkManagementNodesAdapter
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.NodesItemBackgroundDecoration
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.NodesItemDividerDecoration
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.items.NetworkNodeRvItem
import javax.inject.Inject

private val NODES_GROUP_TEXT_STYLE = R.style.TextAppearance_NovaFoundation_SemiBold_Caps2
private val NODES_GROUP_COLOR_RES = R.color.text_secondary
private val NODES_GROUP_TEXT_PADDING_DP = ViewSpace(16, 24, 16, 0)

class ChainNetworkManagementFragment :
    BaseFragment<ChainNetworkManagementViewModel, FragmentChainNetworkManagementBinding>(),
    ChainNetworkManagementHeaderAdapter.ItemHandler,
    ChainNetworkManagementNodesAdapter.ItemHandler {

    companion object {

        private const val EXTRA_PAYLOAD = "payload"

        fun getBundle(payload: ChainNetworkManagementPayload): Bundle {
            return Bundle().apply {
                putParcelable(EXTRA_PAYLOAD, payload)
            }
        }
    }

    override fun createBinding() = FragmentChainNetworkManagementBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val headerAdapter by lazy { ChainNetworkManagementHeaderAdapter(imageLoader, this) }
    private val customNodesTitleAdapter by lazy {
        TextAdapter(
            requireContext().getString(R.string.network_management_custom_nodes),
            NODES_GROUP_TEXT_STYLE,
            NODES_GROUP_COLOR_RES,
            NODES_GROUP_TEXT_PADDING_DP
        )
    }
    private val customNodes by lazy { ChainNetworkManagementNodesAdapter(this) }
    private val defaultNodesTitleAdapter by lazy {
        TextAdapter(
            requireContext().getString(R.string.network_management_default_nodes),
            NODES_GROUP_TEXT_STYLE,
            NODES_GROUP_COLOR_RES,
            NODES_GROUP_TEXT_PADDING_DP
        )
    }
    private val defaultNodes by lazy { ChainNetworkManagementNodesAdapter(this) }

    private val adapter by lazy {
        ConcatAdapter(
            headerAdapter,
            customNodesTitleAdapter,
            customNodes,
            defaultNodesTitleAdapter,
            defaultNodes
        )
    }

    override fun initViews() {
        binder.chainNetworkManagementToolbar.applyStatusBarInsets()
        binder.chainNetworkManagementToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.chainNetworkManagementToolbar.setRightActionClickListener { viewModel.networkActionsClicked() }

        binder.chainNetworkManagementContent.adapter = adapter
        binder.chainNetworkManagementContent.itemAnimator = null
        binder.chainNetworkManagementContent.addItemDecoration(NodesItemBackgroundDecoration(requireContext()))
        binder.chainNetworkManagementContent.addItemDecoration(NodesItemDividerDecoration(requireContext()))
    }

    override fun inject() {
        FeatureUtils.getFeature<SettingsFeatureComponent>(
            requireContext(),
            SettingsFeatureApi::class.java
        )
            .chainNetworkManagementFactory()
            .create(this, argument(EXTRA_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ChainNetworkManagementViewModel) {
        setupListSelectorMixin(viewModel.listSelectorMixin)

        viewModel.isNetworkEditable.observe {
            if (it) {
                binder.chainNetworkManagementToolbar.setRightActionTint(R.color.icon_primary)
                binder.chainNetworkManagementToolbar.setRightIconRes(R.drawable.ic_more_horizontal)
            }
        }

        viewModel.isNetworkCanBeDisabled.observe {
            headerAdapter.setNetworkCanBeDisabled(it)
        }

        viewModel.chainEnabled.observe {
            headerAdapter.setChainEnabled(it)
        }

        viewModel.autoBalanceEnabled.observe {
            headerAdapter.setAutoBalanceEnabled(it)
        }

        viewModel.chainModel.observe {
            headerAdapter.setChainUiModel(it)
        }

        viewModel.customNodes.observe {
            customNodes.submitList(it)
        }

        viewModel.defaultNodes.observe {
            defaultNodesTitleAdapter.show(it.isNotEmpty())
            defaultNodes.submitList(it)
        }

        viewModel.confirmAccountDeletion.awaitableActionLiveData.observeEvent {
            warningDialog(
                requireContext(),
                onPositiveClick = { it.onSuccess(true) },
                onNegativeClick = { it.onSuccess(false) },
                positiveTextRes = R.string.common_delete
            ) {
                setTitle(it.payload.first)
                setMessage(it.payload.second)
            }
        }
    }

    override fun chainEnableClicked() {
        viewModel.chainEnableClicked()
    }

    override fun autoBalanceClicked() {
        viewModel.autoBalanceClicked()
    }

    override fun selectNode(item: NetworkNodeRvItem) {
        viewModel.selectNode(item)
    }

    override fun editNode(item: NetworkNodeRvItem) {
        viewModel.nodeActionClicked(item)
    }

    override fun addNewNode() {
        viewModel.addNewNode()
    }
}
