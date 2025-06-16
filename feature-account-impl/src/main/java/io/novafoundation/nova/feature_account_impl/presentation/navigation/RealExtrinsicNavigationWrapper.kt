package io.novafoundation.nova.feature_account_impl.presentation.navigation

import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
import io.novafoundation.nova.feature_account_api.data.signer.isDelayed
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter

class RealExtrinsicNavigationWrapper(
    private val accountRouter: AccountRouter,
    private val accountUseCase: AccountInteractor
) : ExtrinsicNavigationWrapper {

    override suspend fun startNavigation(
        submissionHierarchy: SubmissionHierarchy,
        fallback: suspend () -> Unit
    ) {
        if (submissionHierarchy.isDelayed()) {
            val delayedAccount = submissionHierarchy.firstDelayedAccount()

            accountUseCase.selectMetaAccount(delayedAccount.id)

            if (delayedAccount.type == LightMetaAccount.Type.MULTISIG) {
                accountRouter.openMainWithFinishMultisigTransaction()
            } else {
                accountRouter.openMain()
            }
        } else {
            fallback()
        }
    }

    private fun SubmissionHierarchy.firstDelayedAccount(): MetaAccount {
        return path.first { it.callExecutionType == CallExecutionType.DELAYED }
            .account
    }
}
