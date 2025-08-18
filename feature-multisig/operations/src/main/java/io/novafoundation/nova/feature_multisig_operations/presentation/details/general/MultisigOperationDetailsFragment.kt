package io.novafoundation.nova.feature_multisig_operations.presentation.details.general

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.isGone
import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.isLoaded
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.onLoaded
import io.novafoundation.nova.common.mixin.actionAwaitable.setupConfirmationDialog
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.common.view.bindWithHideShowButton
import io.novafoundation.nova.common.view.setExtraInfoAvailable
import io.novafoundation.nova.common.view.bottomSheet.action.observeActionBottomSheet
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_account_api.view.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showAddressOrHide
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.feature_multisig_operations.databinding.FragmentMultisigOperationDetailsBinding
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureApi
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureComponent
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallDetailsModel
import io.novafoundation.nova.feature_multisig_operations.presentation.details.general.adapter.SignatoriesAdapter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.amount.setAmountOrHide

class MultisigOperationDetailsFragment : BaseFragment<MultisigOperationDetailsViewModel, FragmentMultisigOperationDetailsBinding>() {

    companion object : PayloadCreator<MultisigOperationDetailsPayload> by FragmentPayloadCreator()

    override fun createBinding() = FragmentMultisigOperationDetailsBinding.inflate(layoutInflater)

    private val adapter by lazy(LazyThreadSafetyMode.NONE) { SignatoriesAdapter(viewModel::onSignatoryClicked) }

    override fun initViews() {
        binder.multisigPendingOperationDetailsToolbar.setHomeButtonIcon(viewModel.getNavigationIconRes())
        binder.multisigPendingOperationDetailsToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.multisigOperationSignatories.adapter = adapter

        binder.multisigPendingOperationDetailsEnterCallData.setOnClickListener { viewModel.enterCallDataClicked() }
        binder.multisigPendingOperationDetailsAction.prepareForProgress(viewLifecycleOwner)
        binder.multisigPendingOperationDetailsAction.setOnClickListener { viewModel.actionClicked() }

        binder.multisigPendingOperationCallDetails.setOnClickListener { viewModel.callDetailsClicked() }
        binder.multisigPendingOperationCallDetails.background = with(requireContext()) {
            addRipple(getBlockDrawable())
        }

        binder.multisigPendingOperationDetailsWallet.setOnClickListener { viewModel.walletDetailsClicked() }
        binder.multisigPendingOperationDetailsBehalfOf.setOnClickListener { viewModel.behalfOfClicked() }
        binder.multisigPendingOperationDetailsSignatory.setOnClickListener { viewModel.signatoryDetailsClicked() }

        binder.multisigOperationSignatoriesContainer.bindWithHideShowButton(binder.multisigOperationShowHideButton)
    }

    override fun inject() {
        FeatureUtils.getFeature<MultisigOperationsFeatureComponent>(
            requireContext(),
            MultisigOperationsFeatureApi::class.java
        )
            .multisigOperationDetails()
            .create(this, payload())
            .inject(this)
    }

    override fun subscribe(viewModel: MultisigOperationDetailsViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel.feeLoaderMixin, binder.multisigPendingOperationDetailsFee)
        observeActionBottomSheet(viewModel.actionBottomSheetLauncher)
        setupConfirmationDialog(R.style.AccentAlertDialogTheme, viewModel.operationNotFoundAwaitableAction)

        viewModel.isOperationLoadingFlow.observe {
            binder.multisigPendingOperationProgress.isVisible = it
            binder.multisigPendingOperationDetailsContainer.isGone = it
        }

        viewModel.showCallButtonState.observe(binder.multisigPendingOperationDetailsEnterCallData::isVisible::set)
        viewModel.actionButtonState.observe(binder.multisigPendingOperationDetailsAction::setState)
        viewModel.buttonAppearance.observe(binder.multisigPendingOperationDetailsAction::setAppearance)

        viewModel.chainUiFlow.observe(binder.multisigPendingOperationDetailsNetwork::showChain)
        viewModel.walletFlow.observe(binder.multisigPendingOperationDetailsWallet::showWallet)

        viewModel.formattedCall.observe {
            binder.multisigPendingOperationDetailsBehalfOf.showAddressOrHide(it.onBehalfOf)
            binder.multisigPendingOperationDetailsToolbar.setTitle(it.title)
            binder.multisigPendingOperationPrimaryAmount.setAmountOrHide(it.primaryAmount)

            showFormattedCallTable(it.tableEntries)
        }

        viewModel.signatoryAccount.observe(binder.multisigPendingOperationDetailsSignatory::showWallet)

        viewModel.signatoriesTitle.observe(binder.multisigOperationSignatoriesTitle::setText)
        viewModel.formattedSignatories.observe { signatoriesLoadingState ->
            binder.multisigOperationSignatoriesShimmering.isVisible = signatoriesLoadingState.isLoading
            binder.multisigOperationSignatories.isVisible = signatoriesLoadingState.isLoaded()
            signatoriesLoadingState.onLoaded { adapter.submitList(it) }
        }

        viewModel.callDetailsVisible.observe(binder.multisigPendingOperationCallDetails::setVisible)
    }

    private fun showFormattedCallTable(tableEntries: List<MultisigCallDetailsModel.TableEntry>) {
        binder.multisigPendingOperationDetailsCallTable.removeAllViews()

        tableEntries.forEach {
            val entryView = createFormattedCallEntryView(it)
            binder.multisigPendingOperationDetailsCallTable.addView(entryView)
        }

        binder.multisigPendingOperationDetailsCallTable.invalidateChildrenVisibility()
    }

    private fun createFormattedCallEntryView(entry: MultisigCallDetailsModel.TableEntry): TableCellView {
        return TableCellView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            setTitle(entry.name)

            when (val value = entry.value) {
                is MultisigCallDetailsModel.TableValue.Account -> {
                    setExtraInfoAvailable(true)
                    showAddress(value.addressModel)
                    setOnClickListener { viewModel.onTableAccountClicked(value) }
                }
            }
        }
    }
}
