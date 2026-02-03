package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.create

import android.text.style.ImageSpan
import android.view.View
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.buildSpannable
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.insets.applySystemBarInsets
import io.novafoundation.nova.common.utils.insets.ImeInsetsState
import io.novafoundation.nova.common.utils.sendEmailIntent
import io.novafoundation.nova.common.utils.setTabSelectedListener
import io.novafoundation.nova.common.utils.setupWithViewPager2
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.FragmentCreateWatchWalletBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent

private const val CUSTOM_ACCOUNT_INDEX = 0
private const val DEMO_ACCOUNT_INDEX = 1

class CreateWatchWalletFragment : BaseFragment<CreateWatchWalletViewModel, FragmentCreateWatchWalletBinding>() {

    override fun createBinding() = FragmentCreateWatchWalletBinding.inflate(layoutInflater)

    override fun applyInsets(rootView: View) {
        binder.createWatchWalletContainer.applySystemBarInsets(imeInsets = ImeInsetsState.ENABLE_IF_SUPPORTED)
    }

    override fun initViews() {
        binder.createWatchWalletToolbar.setHomeButtonListener { viewModel.homeButtonClicked() }

        val adapter = CreateWatchWalletPagerAdapter(viewModel.pages, viewLifecycleOwner.lifecycleScope)
        binder.createWatchWalletViewPager.adapter = adapter
        binder.createWatchWalletMode.setupWithViewPager2(binder.createWatchWalletViewPager, adapter::getPageTitle)

        binder.createWatchWalletMode.setTabSelectedListener {
            when (it.position) {
                CUSTOM_ACCOUNT_INDEX -> viewModel.customAccountSelected()
                DEMO_ACCOUNT_INDEX -> viewModel.demoAccountSelected()
            }
        }

        binder.createWatchWalletTerms.setOnCheckedChangeListener { _, isChecked -> viewModel.onTermsChecked(isChecked) }

        binder.createWatchWalletContinue.setOnClickListener { viewModel.nextClicked() }

        binder.createWatchWalletTerms.setText(getTermsText())
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .createWatchOnlyComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: CreateWatchWalletViewModel) {
        viewModel.buttonState.observe(binder.createWatchWalletContinue::setState)

        viewModel.scamWarningEvent.observeEvent {
            runWarning(it)
        }

        viewModel.openEmailEvent.observeEvent { requireContext().sendEmailIntent(it) }
    }

    private fun getTermsText() = buildSpannedString {
        append(getString(R.string.create_wo_wallet_terms))
        appendLine()
        val highlightColor = requireContext().getColor(R.color.text_negative)
        val highlightedPart = getString(R.string.create_wo_wallet_terms_highlighted).toSpannable(colorSpan(highlightColor))
        append(highlightedPart)
    }

    private fun runWarning(payload: ScamRiskWarningBottomSheet.Payload) {
        val bottomSheet = ScamRiskWarningBottomSheet(
            requireContext(),
            payload,
            viewModel::onWarningConfirmed,
            viewModel::onMailClicked
        )

        bottomSheet.show()
    }
}
