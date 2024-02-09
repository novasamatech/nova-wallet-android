package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.GenericDecimalFee
import java.math.BigDecimal
import java.math.BigInteger

typealias AmountProducer<P> = suspend (P) -> BigDecimal

typealias PlanksProducer<P> = suspend (P) -> BigInteger

typealias FeeProducer<P> = suspend (P) -> DecimalFee?

typealias GenericFeeProducer<F, P> = suspend (P) -> GenericDecimalFee<F>?

typealias GenericFeeListProducer<F, P> = suspend (P) -> List<GenericDecimalFee<F>>
