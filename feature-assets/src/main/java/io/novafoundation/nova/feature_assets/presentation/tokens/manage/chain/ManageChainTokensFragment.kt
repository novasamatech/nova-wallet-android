package io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain

import androidx.core.os.bundleOf

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadTokenIcon
import io.novafoundation.nova.feature_assets.databinding.FragmentManageChainTokensBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent

import javax.inject.Inject

class ManageChainTokensFragment :
    BaseBottomSheetFragment<ManageChainTokensViewModel, FragmentManageChainTokensBinding>(),
    ManageChainTokensAdapter.ItemHandler {

    companion object {

        private const val KEY_PAYLOAD = "ManageChainTokensFragment.Payload"

        fun getBundle(payload: ManageChainTokensPayload) = bundleOf(KEY_PAYLOAD to payload)
    }

    override fun createBinding() = FragmentManageChainTokensBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val tokensAdapter by lazy(LazyThreadSafetyMode.NONE) {
        ManageChainTokensAdapter(imageLoader, this)
    }

    override fun initViews() {
        binder.manageChainTokenChains.setHasFixedSize(true)
        binder.manageChainTokenChains.adapter = tokensAdapter
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .manageChainTokensComponentFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ManageChainTokensViewModel) {
        viewModel.headerModel.observe { headerModel ->
            binder.manageChainTokenIcon.loadTokenIcon(headerModel.icon, imageLoader)
            binder.manageChainTokenSymbol.text = headerModel.symbol
            binder.manageChainTokenSubtitle.text = headerModel.networks
        }

        viewModel.chainInstanceModels.observe(tokensAdapter::submitList)
    }

    override fun enableSwitched(position: Int) {
        viewModel.enableChainSwitchClicked(position)
    }
}
