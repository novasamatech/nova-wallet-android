package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.ViewSpace
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.recyclerview.adapter.text.TextAdapter
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.headerAdapter.ChainNetworkManagementHeaderAdapter
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.ChainNetworkManagementNodesAdapter
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.NodesItemBackgroundDecoration
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.NodesItemDividerDecoration
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.items.NetworkNodeRvItem
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_chain_network_management.chainNetworkManagementContent
import kotlinx.android.synthetic.main.fragment_chain_network_management.chainNetworkManagementToolbar

private val NODES_GROUP_TEXT_STYLE = R.style.TextAppearance_NovaFoundation_SemiBold_Caps2
private val NODES_GROUP_COLOR_RES = R.color.text_secondary
private val NODES_GROUP_TEXT_PADDING_DP = ViewSpace(16, 24, 16, 0)


class ChainNetworkManagementFragment : BaseFragment<ChainNetworkManagementViewModel>(),
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_chain_network_management, container, false)
    }

    override fun initViews() {
        chainNetworkManagementToolbar.applyStatusBarInsets()
        chainNetworkManagementToolbar.setHomeButtonListener { viewModel.backClicked() }

        chainNetworkManagementContent.adapter = adapter
        chainNetworkManagementContent.itemAnimator = null
        chainNetworkManagementContent.addItemDecoration(NodesItemBackgroundDecoration(requireContext()))
        chainNetworkManagementContent.addItemDecoration(NodesItemDividerDecoration(requireContext()))
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
            defaultNodes.submitList(it)
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
        viewModel.editNode(item)
    }

    override fun addNewNode() {
        viewModel.addNewNode()
    }
}
