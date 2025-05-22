package io.novafoundation.nova.feature_account_migration.utils

import io.novafoundation.nova.feature_account_migration.utils.stateMachine.ExchangePayload

class AccountExchangePayload(
    val accountName: String?
) : ExchangePayload
