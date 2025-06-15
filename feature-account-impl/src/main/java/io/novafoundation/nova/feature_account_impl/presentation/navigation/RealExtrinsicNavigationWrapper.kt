package io.novafoundation.nova.feature_account_impl.presentation.navigation

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
            val multisigAccount = submissionHierarchy.firstMultisigOrNull()

            if (multisigAccount == null) {
                accountRouter.openMain()
            } else {
                accountUseCase.selectMetaAccount(multisigAccount.id)
                accountRouter.finishMultisigTransaction()
            }
        } else {
            fallback()
        }
    }

    override suspend fun startNavigation(
        multiSubmissionHierarchy: List<SubmissionHierarchy>,
        fallback: suspend () -> Unit
    ) {
        val firstSubmission = multiSubmissionHierarchy.firstOrNull()

        if (firstSubmission == null) {
            fallback()
        } else {
            startNavigation(firstSubmission, fallback)
        }
    }

    private fun SubmissionHierarchy.firstMultisigOrNull(): MetaAccount? {
        return path.firstOrNull { it.account.type == LightMetaAccount.Type.MULTISIG }
            ?.account
    }
}
