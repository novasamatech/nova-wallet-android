package io.novafoundation.nova.app.root.presentation.legal

import android.view.LayoutInflater
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.databinding.BottomSheetLegalConsentBinding
import io.novafoundation.nova.app.root.di.RootApi
import io.novafoundation.nova.app.root.di.RootComponent
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.formatting.applyTermsAndPrivacyPolicy
import io.novafoundation.nova.common.view.ButtonState

/**
 * Non-dismissible sheet that asks the user to accept the updated legal documents.
 * It cannot be swiped away, dismissed by an outside touch or by the back button - the only way out is to accept.
 */
class LegalConsentBottomSheet : BaseBottomSheetFragment<LegalConsentViewModel, BottomSheetLegalConsentBinding>() {

    override fun createBinding() = BottomSheetLegalConsentBinding.inflate(LayoutInflater.from(context))

    override fun initViews() {
        // Not cancellable: blocks the back button and the outside touch, isHideable blocks the swipe-down
        isCancelable = false
        dialog?.setCanceledOnTouchOutside(false)
        getBehaviour().isHideable = false

        binder.legalConsentText.applyTermsAndPrivacyPolicy(
            containerResId = R.string.legal_consent_agreement,
            termsResId = R.string.common_terms_of_service,
            privacyResId = R.string.common_privacy_notice,
            termsClicked = viewModel::termsClicked,
            privacyClicked = viewModel::privacyClicked,
            underlineLinks = true
        )

        binder.legalConsentCheckBox.setOnCheckedChangeListener { _, isChecked -> viewModel.consentCheckChanged(isChecked) }
        binder.legalConsentAcceptButton.setOnClickListener { viewModel.acceptClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<RootComponent>(requireContext(), RootApi::class.java)
            .legalConsentComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: LegalConsentViewModel) {
        observeBrowserEvents(viewModel)

        viewModel.canProceed.observe { canProceed ->
            binder.legalConsentAcceptButton.setState(if (canProceed) ButtonState.NORMAL else ButtonState.DISABLED)
        }

        viewModel.closeEvent.observeEvent { dismiss() }
    }
}
