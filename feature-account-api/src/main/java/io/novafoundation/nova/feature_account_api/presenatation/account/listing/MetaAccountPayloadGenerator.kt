package io.novafoundation.nova.feature_account_api.presenatation.account.listing

import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi

object MetaAccountPayloadGenerator : PayloadGenerator<AccountUi>(
    AccountUi::title,
    AccountUi::subtitle,
    AccountUi::isSelected
)
