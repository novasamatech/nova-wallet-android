package io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface SelectWalletMixin {

    interface Factory {

        fun create(
            coroutineScope: CoroutineScope,
            selectionParams: suspend () -> SelectionParams
        ): SelectWalletMixin
    }

    class SelectionParams(
        val selectionAllowed: Boolean,
        val initialSelection: InitialSelection
    ) {

        companion object {

            fun default(): SelectionParams = SelectionParams(
                selectionAllowed = true,
                initialSelection = InitialSelection.ActiveWallet
            )
        }
    }

    sealed interface InitialSelection {

        object ActiveWallet : InitialSelection

        class SpecificWallet(val metaId: Long) : InitialSelection
    }

    val selectedMetaAccountFlow: Flow<MetaAccount>

    val selectedWalletModelFlow: Flow<SelectedWalletModel>

    fun walletSelectorClicked()
}

suspend fun SelectWalletMixin.selectedMetaAccount(): MetaAccount {
    return selectedMetaAccountFlow.first()
}
