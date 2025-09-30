package io.novafoundation.nova.common.domain.balance

import io.novafoundation.nova.common.utils.atLeastZero
import java.math.BigInteger

fun legacyTransferable(free: BigInteger, frozen: BigInteger): BigInteger {
    return (free - frozen).atLeastZero()
}

fun holdAndFreezesTransferable(free: BigInteger, frozen: BigInteger, reserved: BigInteger): BigInteger {
    val freeCannotDropBelow = (frozen - reserved).atLeastZero()

    return (free - freeCannotDropBelow).atLeastZero()
}

fun totalBalance(free: BigInteger, reserved: BigInteger): BigInteger {
    return free + reserved
}

// https://github.com/paritytech/polkadot-sdk/blob/b9fbf243c57939ecadc89b82ed42249703203874/substrate/frame/balances/src/impl_currency.rs#L522
// free - amount >= max(ed, frozen) => max_amount = free - max(ed, frozen)
fun legacyReservable(free: BigInteger, frozen: BigInteger, ed: BigInteger): BigInteger {
    return free - ed.max(frozen)
}

// reducible_balance (https://github.com/paritytech/polkadot-sdk/blob/b9fbf243c57939ecadc89b82ed42249703203874/substrate/frame/balances/src/impl_fungible.rs#L47)
// is called with Force and Protect args (https://github.com/paritytech/polkadot-sdk/blob/b9fbf243c57939ecadc89b82ed42249703203874/substrate/frame/support/src/traits/tokens/fungibles/hold.rs#L101)
fun holdsAndFreezesReservable(free: BigInteger, ed: BigInteger): BigInteger {
    return free - ed
}
