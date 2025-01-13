package io.novafoundation.nova.feature_assets.presentation.flow.network

import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.recyclerview.adapter.text.TextAdapter
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.FragmentNetworkFlowBinding
import io.novafoundation.nova.feature_assets.presentation.flow.network.model.NetworkFlowRvItem
import io.novafoundation.nova.feature_assets.presentation.receive.view.LedgerNotSupportedWarningBottomSheet
import javax.inject.Inject

abstract class NetworkFlowFragment<T : NetworkFlowViewModel> :
    BaseFragment<T, FragmentNetworkFlowBinding>(),
    NetworkFlowAdapter.ItemNetworkHandler {

    companion object : PayloadCreator<NetworkFlowPayload> by FragmentPayloadCreator()

    override fun createBinding() = FragmentNetworkFlowBinding.inflate(layoutInflater)

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

    override fun initViews() {
        binder.networkFlowToolbar.applyStatusBarInsets()
        binder.networkFlowToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.networkFlowList.setHasFixedSize(true)
        binder.networkFlowList.adapter = adapter
        binder.networkFlowList.itemAnimator = null
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
