package io.novafoundation.nova.feature_account_api.domain.validation

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isFalseOrWarning
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.domain.account.system.SystemAccountMatcher
import io.novafoundation.nova.feature_account_api.domain.account.system.default
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class SystemAccountRecipientValidation<P, E>(
    private val accountId: (P) -> AccountId?,
    private val error: (AccountId) -> E,
    private val matcher: SystemAccountMatcher = SystemAccountMatcher.default(),
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val accountId = accountId(value) ?: return valid()

        return matcher.isSystemAccount(accountId) isFalseOrWarning {
            error(accountId)
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.notSystemAccount(
    accountId: (P) -> AccountId?,
    error: (AccountId) -> E,
) {
    validate(SystemAccountRecipientValidation(accountId, error))
}

fun handleSystemAccountValidationFailure(resourceManager: ResourceManager): TitleAndMessage {
    return resourceManager.getString(R.string.send_recipient_system_account_title) to
        resourceManager.getString(R.string.send_recipient_system_account_message)
}
