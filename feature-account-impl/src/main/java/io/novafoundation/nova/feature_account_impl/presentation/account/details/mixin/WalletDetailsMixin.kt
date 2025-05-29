package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin

import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.details.ChainAccountActionsSheet.AccountAction
import kotlinx.coroutines.flow.Flow

class WalletDetailsMixinHost(
    val browserableDelegate: Browserable.Presentation
)

abstract class WalletDetailsMixin(
    val metaAccount: MetaAccount
) {

    abstract val availableAccountActions: Flow<Set<AccountAction>>

    abstract val typeAlert: Flow<AlertModel?>

    // List<ChainAccountGroupUi | AccountInChainUi>
    abstract fun accountProjectionsFlow(): Flow<List<Any?>>

    open suspend fun groupActionClicked(groupId: String) {}
}
