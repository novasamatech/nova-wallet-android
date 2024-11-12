package io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import kotlinx.android.synthetic.main.fragment_manage_chain_tokens.manageChainTokenChains
import kotlinx.android.synthetic.main.fragment_manage_chain_tokens.manageChainTokenIcon
import kotlinx.android.synthetic.main.fragment_manage_chain_tokens.manageChainTokenSubtitle
import kotlinx.android.synthetic.main.fragment_manage_chain_tokens.manageChainTokenSymbol
import javax.inject.Inject

class ManageChainTokensFragment :
    BaseBottomSheetFragment<ManageChainTokensViewModel>(),
    ManageChainTokensAdapter.ItemHandler {

    companion object {

        private const val KEY_PAYLOAD = "ManageChainTokensFragment.Payload"

        fun getBundle(payload: ManageChainTokensPayload) = bundleOf(KEY_PAYLOAD to payload)
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    private val tokensAdapter by lazy(LazyThreadSafetyMode.NONE) {
        ManageChainTokensAdapter(imageLoader, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_manage_chain_tokens, container, false)
    }

    override fun initViews() {
        manageChainTokenChains.setHasFixedSize(true)
        manageChainTokenChains.adapter = tokensAdapter
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .manageChainTokensComponentFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ManageChainTokensViewModel) {
        viewModel.headerModel.observe { headerModel ->
            manageChainTokenIcon.setTokenIcon(headerModel.icon, imageLoader)
            manageChainTokenSymbol.text = headerModel.symbol
            manageChainTokenSubtitle.text = headerModel.networks
        }

        viewModel.chainInstanceModels.observe(tokensAdapter::submitList)
    }

    override fun enableSwitched(position: Int) {
        viewModel.enableChainSwitchClicked(position)
    }
}
