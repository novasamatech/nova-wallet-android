package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.start

import android.os.Bundle
import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setupWithViewPager2
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.databinding.FragmentImportParitySignerStartBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.ParitySignerStartPayload

class StartImportParitySignerFragment : BaseFragment<StartImportParitySignerViewModel, FragmentImportParitySignerStartBinding>() {

    companion object {

        private const val PAYLOAD_KEY = "StartImportParitySignerFragment.Payload"

        fun getBundle(payload: ParitySignerStartPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    private val pageAdapter by lazy(LazyThreadSafetyMode.NONE) { StartImportParitySignerPagerAdapter(viewModel.pages) }

    override fun createBinding() = FragmentImportParitySignerStartBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.startImportParitySignerToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.startImportParitySignerScanQrCode.setOnClickListener { viewModel.scanQrCodeClicked() }

        binder.startImportParitySignerMode.isVisible = pageAdapter.itemCount > 1
        binder.startImportParitySignerPages.adapter = pageAdapter
        binder.startImportParitySignerMode.setupWithViewPager2(binder.startImportParitySignerPages, pageAdapter::getPageTitle)
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .startImportParitySignerComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: StartImportParitySignerViewModel) {
        binder.startImportParitySignerTitle.text = viewModel.title
        pageAdapter.setTargetImage(viewModel.polkadotVaultVariantIcon)
    }
}
