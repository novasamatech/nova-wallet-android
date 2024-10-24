package io.novafoundation.nova.feature_assets.presentation.flow.network

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.FragmentPayloadHolder
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.recyclerview.adapter.text.TextAdapter
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.flow.network.model.NetworkFlowRvItem
import io.novafoundation.nova.feature_assets.presentation.receive.view.LedgerNotSupportedWarningBottomSheet
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_asset_flow_search.assetFlowToolbar
import kotlinx.android.synthetic.main.fragment_network_flow.networkFlowList
import kotlinx.android.synthetic.main.fragment_network_flow.networkFlowToolbar

abstract class NetworkFlowFragment<T : NetworkFlowViewModel> :
    BaseFragment<T>(),
    FragmentPayloadHolder<NetworkFlowPayload>,
    NetworkFlowAdapter.ItemNetworkHandler {

    companion object : PayloadCreator<NetworkFlowPayload> by FragmentPayloadCreator()

    @Inject
    lateinit var imageLoader: ImageLoader

    private val titleAdapter by lazy(LazyThreadSafetyMode.NONE) {
        TextAdapter(styleRes = R.style.TextAppearance_NovaFoundation_Bold_Title3)
    }

    private val networkAdapter by lazy(LazyThreadSafetyMode.NONE) {
        NetworkFlowAdapter(imageLoader, this)
    }

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        ConcatAdapter(titleAdapter, networkAdapter)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_network_flow, container, false)
    }

    fun setTitle(@StringRes titleRes: Int) {
        assetFlowToolbar.setTitle(titleRes)
    }

    override fun initViews() {
        networkFlowToolbar.applyStatusBarInsets()
        networkFlowToolbar.setHomeButtonListener { viewModel.backClicked() }

        networkFlowList.setHasFixedSize(true)
        networkFlowList.adapter = adapter
        networkFlowList.itemAnimator = null
    }

    override fun subscribe(viewModel: T) {
        viewModel.titleFlow.observe {
            titleAdapter.setText(it)
        }

        viewModel.networks.observe {
            networkAdapter.submitList(it)
        }

        viewModel.acknowledgeLedgerWarning.awaitableActionLiveData.observeEvent {
            LedgerNotSupportedWarningBottomSheet(
                context = requireContext(),
                onSuccess = { it.onSuccess(Unit) },
                message = it.payload
            ).show()
        }
    }

    override fun networkClicked(network: NetworkFlowRvItem) {
        viewModel.networkClicked(network)
    }
}
