package jp.co.soramitsu.feature_account_api.domain.model

import jp.co.soramitsu.runtime.multiNetwork.chain.model.ChainId

sealed class AddAccountType {

    object MetaAccount : AddAccountType()

    class ChainAccount(val chainId: ChainId, val metaId: Long): AddAccountType()
}
