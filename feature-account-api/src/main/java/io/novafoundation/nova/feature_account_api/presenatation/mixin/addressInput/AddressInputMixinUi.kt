package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.keyboard.isKeyboardVisible
import io.novafoundation.nova.common.utils.keyboard.setKeyboardVisibilityListener
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.presenatation.account.external.ExternalAccountsBottomSheet
import io.novafoundation.nova.web3names.domain.exceptions.Web3NamesException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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

fun BaseFragment<*>.setupExternalAccounts(
    mixin: AddressInputMixin,
    inputField: AddressInputField,
    chainNameFlow: Flow<String>
) {
    setupExternalAccountsCallback(mixin, inputField)
    setupExternalSelectedAccount(mixin, inputField, chainNameFlow)
    setupExternalAccountsBottomSheet(mixin, chainNameFlow)
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
            mixin.onInputFocusChanged()
        }
    }

    lifecycle.setKeyboardVisibilityListener(inputField) { keyboardVisible ->
        if (!keyboardVisible && inputField.content.hasFocus()) {
            mixin.onKeyboardGone()
        }
    }
}

private fun BaseFragment<*>.setupExternalSelectedAccount(
    mixin: AddressInputMixin,
    inputField: AddressInputField,
    chainNameFlow: Flow<String>
) {
    mixin.selectedExternalIdentifierFlow.observeWhenVisible {
        inputField.setExternalAccount(it)

        val chainName = chainNameFlow.first()
        val selectedW3NIdentifier = mixin.getExternalAccountIdentifier()
        if (it is ExtendedLoadingState.Error) {
            val titleAndMessage = when (it.exception) {
                is Web3NamesException.ChainProviderNotFoundException -> {
                    getString(R.string.web3names_invalid_recepient_title) to getString(R.string.web3names_recepient_not_found_message, selectedW3NIdentifier)
                }
                is Web3NamesException.ValidAccountNotFoundException -> {
                    getString(R.string.web3names_invalid_recepient_title) to getString(
                        R.string.web3names_no_valid_recepient_found_message,
                        selectedW3NIdentifier,
                        chainName
                    )
                }
                else -> getString(R.string.web3names_service_unavailable_title) to getString(R.string.web3names_service_unavailable_message, chainName)
            }

            showErrorWithTitle(
                titleAndMessage.first,
                titleAndMessage.second
            )
        }
    }
}

private fun BaseFragment<*>.setupExternalAccountsBottomSheet(
    mixin: AddressInputMixin,
    chainNameFlow: Flow<String>
) {
    mixin.showExternalAccountsFlow.observeWhenVisible { externalAccounts ->
        if (externalAccounts.accounts.size <= 1) return@observeWhenVisible

        val externalIdentifier = mixin.getExternalAccountIdentifier()
        val chainName = chainNameFlow.first()

        val bottomSheet = ExternalAccountsBottomSheet(
            requireContext(),
            getString(R.string.web3names_identifiers_sheet_title, chainName.first(), externalIdentifier),
            DynamicListBottomSheet.Payload(externalAccounts.accounts, externalAccounts.selected)
        ) { bottomSheet, account ->
            if (mixin.isValidExternalAccount(account)) {
                mixin.selectExternalAccount(account)
                bottomSheet.dismiss()
            } else {
                showErrorWithTitle(
                    getString(R.string.web3names_invalid_recepient_title),
                    getString(R.string.common_validation_invalid_address_message, chainName)
                )
            }
        }

        bottomSheet.show()
    }
}
