package io.novafoundation.nova.feature_account_api.data.signer

import io.novafoundation.nova.common.utils.MutableSharedState
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.InheritedImplication

typealias SigningSharedState = MutableSharedState<SeparateFlowSignerState>

class SeparateFlowSignerState(val inheritedImplication: InheritedImplication, val metaAccount: MetaAccount)
