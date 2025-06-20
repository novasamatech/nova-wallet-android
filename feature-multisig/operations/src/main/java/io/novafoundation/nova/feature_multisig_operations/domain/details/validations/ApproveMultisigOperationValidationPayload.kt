package io.novafoundation.nova.feature_multisig_operations.domain.details.validations

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.ChainAssetBalance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ApproveMultisigOperationValidationPayload(
    val fee: Fee,
    val signatoryBalance: ChainAssetBalance,
    val chain: Chain,
    val signatory: MetaAccount,
)
