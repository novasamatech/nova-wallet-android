package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter.NetworkManagementListAdapter
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_network_list.networkList

abstract class NetworkListFragment<T : NetworkListViewModel> : BaseFragment<T>() {

    @Inject
    lateinit var imageLoader: ImageLoader

    protected val networksAdapter by lazy(LazyThreadSafetyMode.NONE) { NetworkManagementListAdapter(imageLoader) }

    protected abstract val adapter: RecyclerView.Adapter<*>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_network_list, container, false)
    }

    override fun initViews() {
        networkList.adapter = adapter
    }

    override fun subscribe(viewModel: T) {
        viewModel.networkList.observe { networksAdapter.submitList(it) }
    }
}
