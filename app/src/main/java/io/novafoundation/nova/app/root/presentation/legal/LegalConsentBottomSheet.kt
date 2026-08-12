package io.novafoundation.nova.app.root.presentation.legal

import android.view.LayoutInflater
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.databinding.BottomSheetLegalConsentBinding
import io.novafoundation.nova.app.databinding.ViewLegalConsentRowBinding
import io.novafoundation.nova.app.root.di.RootApi
import io.novafoundation.nova.app.root.di.RootComponent
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.data.legal.LegalDocumentType
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.ButtonState

/**
 * Non-dismissible sheet that asks the user to accept each updated legal document.
 * It cannot be swiped away, dismissed by an outside touch or by the back button - the only way out is to accept.
 */
class LegalConsentBottomSheet : BaseBottomSheetFragment<LegalConsentViewModel, BottomSheetLegalConsentBinding>() {

    override fun createBinding() = BottomSheetLegalConsentBinding.inflate(LayoutInflater.from(context))

    private val rowBindings by lazy(LazyThreadSafetyMode.NONE) {
        mapOf(
            LegalDocumentType.TERMS_OF_SERVICE to binder.legalConsentTermsRow,
            LegalDocumentType.PRIVACY_NOTICE to binder.legalConsentPrivacyRow
        )
    }

    override fun initViews() {
        // Not cancellable: blocks the back button and the outside touch, isHideable blocks the swipe-down
        isCancelable = false
        dialog?.setCanceledOnTouchOutside(false)
        getBehaviour().isHideable = false

        rowBindings.forEach { (type, row) -> setupRow(row, type) }

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

        viewModel.documentModels.observe { models ->
            val shownTypes = models.map(LegalConsentModel::type).toSet()

            rowBindings.forEach { (type, row) -> row.root.setVisible(type in shownTypes) }

            models.forEach { model ->
                rowBindings[model.type]?.legalConsentRowUpdatedAt?.text =
                    getString(R.string.legal_consent_document_updated, model.updatedAt)
            }
        }

        viewModel.canProceed.observe { canProceed ->
            binder.legalConsentAcceptButton.setState(if (canProceed) ButtonState.NORMAL else ButtonState.DISABLED)
        }

        viewModel.closeEvent.observeEvent { dismiss() }
    }

    private fun setupRow(row: ViewLegalConsentRowBinding, type: LegalDocumentType) {
        row.legalConsentRowTitle.setText(titleOf(type))

        row.legalConsentRowCheckBox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.documentCheckChanged(type, isChecked)
        }

        // The chevron affordance: tapping the texts opens the document, the checkbox only toggles consent
        row.legalConsentRowClickableArea.setOnClickListener { viewModel.documentClicked(type) }
    }

    private fun titleOf(type: LegalDocumentType) = when (type) {
        LegalDocumentType.TERMS_OF_SERVICE -> R.string.legal_consent_terms_row
        LegalDocumentType.PRIVACY_NOTICE -> R.string.legal_consent_privacy_row
    }
}
