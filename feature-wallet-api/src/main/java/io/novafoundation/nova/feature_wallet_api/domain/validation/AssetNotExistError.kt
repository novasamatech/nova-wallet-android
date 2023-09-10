package io.novafoundation.nova.feature_wallet_api.domain.validation

typealias AssetNotExistError<E> = (existingSymbol: String) -> E
