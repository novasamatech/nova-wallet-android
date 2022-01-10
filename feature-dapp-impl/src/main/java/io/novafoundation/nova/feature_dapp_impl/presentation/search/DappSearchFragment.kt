package io.novafoundation.nova.feature_dapp_impl.presentation.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import kotlinx.android.synthetic.main.fragment_dapp_extrinsic_details.extrinsicDetailsContent
import kotlinx.android.synthetic.main.fragment_dapp_extrinsic_details.signExtrinsicToolbar
import kotlinx.android.synthetic.main.fragment_search_dapp.searchDappCancel
import kotlinx.android.synthetic.main.fragment_search_dapp.searchDappSearhGroup
import kotlinx.android.synthetic.main.fragment_search_dapp.searhDappQuery

class DappSearchFragment : BaseBottomSheetFragment<DAppSearchViewModel>() {

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {  }

    companion object {

        private const val PAYLOAD_KEY = "PAYLOAD_KEY"

        fun getBundle(extrinsicContent: String) = bundleOf(PAYLOAD_KEY to extrinsicContent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_search_dapp, container, false)
    }

    override fun initViews() {
        searchDappSearhGroup.applyStatusBarInsets()

        searchDappCancel.setOnClickListener { viewModel.cancelClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .extrinsicDetailsComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: DAppSearchViewModel) {
        searhDappQuery.bindTo(viewModel.query, lifecycleScope)

        viewModel.searchResults.observe {
            sea
        }
    }
}
