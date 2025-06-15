package io.novafoundation.nova.feature_account_impl.presentation.navigation

import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
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
        val lastMultisigAccount = submissionHierarchy.lastMultisigOrNull()

        if (lastMultisigAccount == null) {
            fallback()
        } else {
            accountUseCase.selectMetaAccount(lastMultisigAccount.id)

            accountRouter.openMain()

            accountRouter.showMultisigCreatedScreen()
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

    // TODO: we can check delayed operations instead of meta accounts in future
    private fun SubmissionHierarchy.lastMultisigOrNull(): MetaAccount? {
        return path.lastOrNull { it.type == LightMetaAccount.Type.MULTISIG }
    }
}
