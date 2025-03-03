package io.novafoundation.nova.runtime.storage.source.query.api.converters

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageKeyFromInternalBinder
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageKeyToInternalBinder

val AccountIdKey.Companion.scaleEncoder: QueryableStorageKeyToInternalBinder<AccountIdKey>
    get() = AccountIdKey::value

val AccountIdKey.Companion.scaleDecoder: QueryableStorageKeyFromInternalBinder<AccountIdKey>
    get() = ::bindAccountIdKey
