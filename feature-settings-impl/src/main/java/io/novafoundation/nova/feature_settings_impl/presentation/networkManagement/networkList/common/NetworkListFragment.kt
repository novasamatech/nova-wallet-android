package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common

import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.feature_settings_impl.databinding.FragmentNetworkListBinding
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter.NetworkManagementListAdapter
import javax.inject.Inject

abstract class NetworkListFragment<T : NetworkListViewModel> : BaseFragment<T, FragmentNetworkListBinding>(), NetworkManagementListAdapter.ItemHandler {

    override fun createBinding() = FragmentNetworkListBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    protected val networksAdapter by lazy(LazyThreadSafetyMode.NONE) { NetworkManagementListAdapter(imageLoader, this) }

    protected abstract val adapter: RecyclerView.Adapter<*>

    override fun initViews() {
        binder.networkList.adapter = adapter
        binder.networkList.itemAnimator = null
    }

    override fun subscribe(viewModel: T) {
        viewModel.networkList.observe { networksAdapter.submitList(it) }
    }

    override fun onNetworkClicked(chainId: String) {
        viewModel.onNetworkClicked(chainId)
    }
}
