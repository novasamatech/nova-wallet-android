package io.novafoundation.nova.feature_push_notifications.presentation.handling.types.multisig

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdKeyIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkData
import io.novafoundation.nova.feature_push_notifications.presentation.handling.NotificationData
import io.novafoundation.nova.feature_push_notifications.presentation.handling.extractPayloadFieldsWithPath
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.toAccountIdKey
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.extensions.fromHex

class AddressWithAccountId(
    val address: String,
    val accountId: AccountIdKey
)

class MultisigNotificationPayload(
    val multisig: AddressWithAccountId,
    val signatory: AddressWithAccountId,
    val callHashString: String,
    val callHash: AccountIdKey,
    val callData: String?
)

fun String.toAddressWithAccountId(chain: Chain) = AddressWithAccountId(this, this.toAccountIdKey(chain))

fun NotificationData.extractMultisigPayload(signatoryRole: String, chain: Chain): MultisigNotificationPayload {
    val callHashString = extractPayloadFieldsWithPath<String>("callHash")
    return MultisigNotificationPayload(
        extractPayloadFieldsWithPath<String>("multisig").toAddressWithAccountId(chain),
        extractPayloadFieldsWithPath<String>(signatoryRole).toAddressWithAccountId(chain),
        callHashString,
        callHashString.fromHex().intoKey(),
        extractPayloadFieldsWithPath<String?>("callData")
    )
}

suspend fun AccountRepository.getMultisigForPayload(chain: Chain, payload: MultisigNotificationPayload): MultisigMetaAccount? {
    return getActiveMetaAccounts()
        .filterIsInstance<MultisigMetaAccount>()
        .filter { it.accountIdKeyIn(chain) == payload.multisig.accountId }
        .getActorExcept(payload.signatory)
}

fun List<MultisigMetaAccount>.getActorExcept(signatory: AddressWithAccountId): MultisigMetaAccount? {
    return firstOrNull { it.signatoryAccountId != signatory.accountId }
}

fun multisigOperationDeepLinkData(
    metaAccount: MultisigMetaAccount,
    chain: Chain,
    payload: MultisigNotificationPayload,
    operationState: MultisigOperationDeepLinkData.State?
): MultisigOperationDeepLinkData {
    return MultisigOperationDeepLinkData(
        chain.id,
        metaAccount.requireAddressIn(chain),
        chain.addressOf(metaAccount.signatoryAccountId),
        payload.callHashString,
        payload.callData,
        operationState
    )
}
