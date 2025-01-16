package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

data class ComponentHostContext(
    val errorDisplayer: (Throwable) -> Unit,
    val validationExecutor: ValidationExecutor,
    val selectedAccount: Flow<MetaAccount>,
    val assetFlow: Flow<Asset>,
    val scope: ComputationalScope,
    val externalActions: ExternalActions.Presentation
)
