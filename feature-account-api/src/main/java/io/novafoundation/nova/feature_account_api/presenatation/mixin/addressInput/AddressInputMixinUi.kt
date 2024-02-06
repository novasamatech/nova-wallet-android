package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.SingletonDialogHolder
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.keyboard.isKeyboardVisible
import io.novafoundation.nova.common.utils.keyboard.setKeyboardVisibilityListener
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.presenatation.account.external.ExternalAccountsBottomSheet
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.AccountIdentifierProvider.Event.ErrorEvent
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.AccountIdentifierProvider.Event.ShowBottomSheetEvent
import io.novafoundation.nova.web3names.domain.exceptions.Web3NamesException

fun BaseFragment<*>.setupAddressInput(
    mixin: AddressInputMixin,
    inputField: AddressInputField
) = with(inputField) {
    content.bindTo(mixin.inputFlow, lifecycleScope)

    onScanClicked { mixin.scanClicked() }
    onPasteClicked { mixin.pasteClicked() }
    onClearClicked { mixin.clearClicked() }
    onMyselfClicked { mixin.myselfClicked() }

    mixin.state.observe(::setState)
}

/**
 * Make sure that the insets are not consumed by the layer above for this method to work correctly
 */
fun BaseFragment<*>.setupExternalAccounts(
    mixin: AddressInputMixin,
    inputField: AddressInputField
) {
    mixin.selectedExternalAccountFlow.observeWhenVisible { inputField.setExternalAccount(it) }
    handleExternalAccountEvents(mixin)
    setupExternalAccountsCallback(mixin, inputField)
}

private fun BaseFragment<*>.setupExternalAccountsCallback(
    mixin: AddressInputMixin,
    inputField: AddressInputField
) {
    inputField.onExternalAddressClicked {
        mixin.selectedExternalAddressClicked()
    }

    inputField.content.setOnFocusChangeListener { v, hasFocus ->
        if (!hasFocus && isKeyboardVisible()) {
            mixin.loadExternalIdentifiers()
        }
    }

    addInputKeyboardCallback(mixin, inputField)
}

fun BaseFragment<*>.addInputKeyboardCallback(mixin: AddressInputMixin, inputField: AddressInputField) {
    lifecycle.setKeyboardVisibilityListener(inputField) { keyboardVisible ->
        if (!keyboardVisible && inputField.content.hasFocus()) {
            mixin.loadExternalIdentifiers()
        }
    }
}

fun BaseFragment<*>.removeInputKeyboardCallback(inputField: AddressInputField) {
    lifecycle.setKeyboardVisibilityListener(inputField, null)
}

private fun BaseFragment<*>.handleExternalAccountEvents(mixin: AddressInputMixin) {
    val singletonDialogHolder = SingletonDialogHolder<ExternalAccountsBottomSheet>()

    mixin.externalIdentifierEventLiveData.observeEvent {
        when (it) {
            is ShowBottomSheetEvent -> showExternalAccountsBottomSheet(mixin, it, singletonDialogHolder)
            is ErrorEvent -> handleError(it)
        }
    }
}

private fun BaseFragment<*>.showExternalAccountsBottomSheet(
    mixin: AddressInputMixin,
    event: ShowBottomSheetEvent,
    singletonDialogHolder: SingletonDialogHolder<ExternalAccountsBottomSheet>
) {
    singletonDialogHolder.showNewDialogOrSkip {
        ExternalAccountsBottomSheet(
            requireContext(),
            getString(R.string.web3names_identifiers_sheet_title, event.chainName, event.identifier),
            DynamicListBottomSheet.Payload(event.externalAccounts, event.selectedAccount)
        ) { bottomSheet, account ->
            if (account.isValid) {
                mixin.selectExternalAccount(account)
                bottomSheet.dismiss()
            } else {
                showErrorWithTitle(
                    getString(R.string.web3names_invalid_recepient_title),
                    getString(R.string.common_validation_invalid_address_message, event.chainName)
                )
            }
        }
    }
}

private fun BaseFragment<*>.handleError(event: ErrorEvent) {
    val titleAndMessage = when (val exception = event.exception) {
        is Web3NamesException.ChainProviderNotFoundException -> {
            getString(R.string.web3names_invalid_recepient_title) to getString(R.string.web3names_recepient_not_found_message, exception.web3Name)
        }

        is Web3NamesException.ValidAccountNotFoundException -> {
            getString(R.string.web3names_invalid_recepient_title) to getString(
                R.string.web3names_no_valid_recepient_found_message,
                exception.web3Name,
                exception.chainName
            )
        }

        is Web3NamesException.IntegrityCheckFailed -> {
            getString(R.string.web3names_integrity_check_failed_title) to getString(
                R.string.web3names_integrity_check_failed_message,
                exception.web3Name
            )
        }

        is Web3NamesException.UnknownException -> {
            getString(R.string.web3names_service_unavailable_title) to getString(
                R.string.web3names_service_unavailable_message,
                exception.chainName
            )
        }
        is Web3NamesException.UnsupportedAsset -> {
            getString(R.string.web3names_unsupported_asset_title, exception.chainAsset.symbol) to getString(
                R.string.web3names_unsupported_asset_message,
                exception.chainAsset.symbol
            )
        }
    }

    showErrorWithTitle(
        titleAndMessage.first,
        titleAndMessage.second
    )
}
