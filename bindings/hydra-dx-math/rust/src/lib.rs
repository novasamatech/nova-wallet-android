#![allow(non_snake_case)]

extern crate core;
extern crate hydra_dx_math;
extern crate jni;
extern crate serde;
extern crate sp_arithmetic;

use std::collections::HashMap;

use hydra_dx_math::stableswap::types::AssetReserve;
use jni::objects::{JClass, JString};
use jni::sys::jint;
use jni::JNIEnv;
use serde::Deserialize;
use sp_arithmetic::per_things::Permill;
use serde_aux::prelude::*;

fn error() -> String {
    "-1".to_string()
}

macro_rules! parse_into {
    ($x:ty, $y:expr) => {{
        let r = if let Some(x) = $y.parse::<$x>().ok() {
            x
        } else {
                println!("Parse failed");
            return error();
        };
        r
    }};
}

const D_ITERATIONS: u8 = 128;
const Y_ITERATIONS: u8 = 64;

#[derive(Deserialize, Copy, Clone, Debug)]
pub struct AssetBalance {
    asset_id: u32,
    #[serde(deserialize_with = "deserialize_number_from_string")]
    amount: u128,
    decimals: u8,
}

impl From<&AssetBalance> for AssetReserve {
    fn from(value: &AssetBalance) -> Self {
        Self {
            amount: value.amount,
            decimals: value.decimals,
        }
    }
}

#[derive(Deserialize, Copy, Clone, Debug)]
pub struct AssetAmount {
    asset_id: u32,
    #[serde(deserialize_with = "deserialize_number_from_string")]
    amount: u128,
}

// Tuple struct to apply per-field deserializers on u128s
#[derive(Deserialize, Copy, Clone, Debug)]
struct U128Pair(
    #[serde(deserialize_with = "deserialize_number_from_string")] u128,
    #[serde(deserialize_with = "deserialize_number_from_string")] u128,
);

// Parse JSON like: [["0","0"],["1000000000000","500000000000"],["42","1337"]]
fn parse_pairs(json: &str) -> Option<Vec<(u128, u128)>> {
    let v: serde_json::Result<Vec<U128Pair>> = serde_json::from_str(json);
    match v {
        Ok(vecp) => Some(vecp.into_iter().map(|p| (p.0, p.1)).collect()),
        Err(_) => None,
    }
}

fn get_str<'a>(jni: &'a JNIEnv<'a>, string: JString<'a>) -> String {
    jni.get_string(string).unwrap().to_str().unwrap().to_string()
}

/* ---------------- STABLESWAP ---------------- */

#[no_mangle]
pub fn Java_io_novafoundation_nova_hydra_1dx_1math_stableswap_StableSwapMathBridge_calculate_1out_1given_1in<'a>(
    jni_env: JNIEnv<'a>,
    _: JClass,
    reserves: JString,
    asset_in: jint,
    asset_out: jint,
    amount_in: JString,
    amplification: JString,
    fee: JString,
    pegs: JString,
) -> JString<'a> {
    let reserves = get_str(&jni_env, reserves);
    let asset_in = asset_in as u32;
    let asset_out = asset_out as u32;
    let amount_in = get_str(&jni_env, amount_in);
    let amplification = get_str(&jni_env, amplification);
    let fee = get_str(&jni_env, fee);
    let pegs = get_str(&jni_env, pegs);

    let out = calculate_out_given_in(
        reserves,
        asset_in,
        asset_out,
        amount_in,
        amplification,
        fee,
        pegs,
    );

    jni_env.new_string(out).unwrap()
}

fn calculate_out_given_in(
    reserves: String,
    asset_in: u32,
    asset_out: u32,
    amount_in: String,
    amplification: String,
    fee: String,
    pegs: String,
) -> String {
    let reserves: serde_json::Result<Vec<AssetBalance>> = serde_json::from_str(&reserves);
    if reserves.is_err() {
        return error();
    }
    let mut reserves = reserves.unwrap();
    reserves.sort_by_key(|v| v.asset_id);

    let idx_in = reserves.iter().position(|v| v.asset_id == asset_in);
    let idx_out = reserves.iter().position(|v| v.asset_id == asset_out);

    if idx_in.is_none() || idx_out.is_none() {
        return error();
    }

    let amount_in = parse_into!(u128, amount_in);
    let amplification = parse_into!(u128, amplification);
    let fee = Permill::from_float(parse_into!(f64, fee));
    let balances: Vec<AssetReserve> = reserves.iter().map(|v| v.into()).collect();

    // Parse 7th param
    let pairs = match parse_pairs(&pegs) {
        Some(p) => p,
        None => return error(),
    };

    let result = hydra_dx_math::stableswap::calculate_out_given_in_with_fee::<D_ITERATIONS, Y_ITERATIONS>(
            &balances,
            idx_in.unwrap(),
            idx_out.unwrap(),
            amount_in,
            amplification,
            fee,
            &pairs,
        );

    if let Some(r) = result {
        r.0.to_string()
    } else {
        error()
    }
}

#[no_mangle]
pub fn Java_io_novafoundation_nova_hydra_1dx_1math_stableswap_StableSwapMathBridge_calculate_1in_1given_1out<'a>(
    jni_env: JNIEnv<'a>,
    _: JClass,
    reserves: JString,
    asset_in: jint,
    asset_out: jint,
    amount_in: JString,
    amplification: JString,
    fee: JString,
    pegs: JString,
) -> JString<'a> {
    let reserves = get_str(&jni_env, reserves);
    let asset_in = asset_in as u32;
    let asset_out = asset_out as u32;
    let amount_in = get_str(&jni_env, amount_in);
    let amplification = get_str(&jni_env, amplification);
    let fee = get_str(&jni_env, fee);
    let pegs = get_str(&jni_env, pegs);

    let result = calculate_in_given_out(
        reserves,
        asset_in,
        asset_out,
        amount_in,
        amplification,
        fee,
        pegs,
    );

    jni_env.new_string(result).unwrap()
}

fn calculate_in_given_out(
    reserves: String,
    asset_in: u32,
    asset_out: u32,
    amount_out: String,
    amplification: String,
    fee: String,
    pegs: String,
) -> String {
    let reserves: serde_json::Result<Vec<AssetBalance>> = serde_json::from_str(&reserves);
    if reserves.is_err() {
        return error();
    }
    let mut reserves = reserves.unwrap();
    reserves.sort_by_key(|v| v.asset_id);

    let idx_in = reserves.iter().position(|v| v.asset_id == asset_in);
    let idx_out = reserves.iter().position(|v| v.asset_id == asset_out);

    if idx_in.is_none() || idx_out.is_none() {
        return error();
    }

    let amount_out = parse_into!(u128, amount_out);
    let amplification = parse_into!(u128, amplification);
    let fee = Permill::from_float(parse_into!(f64, fee));

    let balances: Vec<AssetReserve> = reserves.iter().map(|v| v.into()).collect();

    // Parse 7th param
    let pairs = match parse_pairs(&pegs) {
        Some(p) => p,
        None => return error(),
    };

    let result =
        hydra_dx_math::stableswap::calculate_in_given_out_with_fee::<D_ITERATIONS, Y_ITERATIONS>(
            &balances,
            idx_in.unwrap(),
            idx_out.unwrap(),
            amount_out,
            amplification,
            fee,
            &pairs,
        );

    if let Some(r) = result {
        r.0.to_string()
    } else {
        error()
    }
}

#[no_mangle]
pub fn Java_io_novafoundation_nova_hydra_1dx_1math_stableswap_StableSwapMathBridge_calculate_1amplification<'a>(
    jni_env: JNIEnv<'a>,
    _: JClass,
    initial_amplification: JString,
    final_amplification: JString,
    initial_block: JString,
    final_block: JString,
    current_block: JString,
) -> JString<'a> {
    let initial_amplification = get_str(&jni_env, initial_amplification);
    let final_amplification = get_str(&jni_env, final_amplification);
    let initial_block = get_str(&jni_env, initial_block);
    let final_block = get_str(&jni_env, final_block);
    let current_block = get_str(&jni_env, current_block);

    let result = calculate_amplification(
        initial_amplification,
        final_amplification,
        initial_block,
        final_block,
        current_block,
    );

    jni_env.new_string(result).unwrap()
}

fn calculate_amplification(
    initial_amplification: String,
    final_amplification: String,
    initial_block: String,
    final_block: String,
    current_block: String,
) -> String {
    let initial_amplification = parse_into!(u128, initial_amplification);
    let final_amplification = parse_into!(u128, final_amplification);
    let initial_block = parse_into!(u128, initial_block);
    let final_block = parse_into!(u128, final_block);
    let current_block = parse_into!(u128, current_block);

    hydra_dx_math::stableswap::calculate_amplification(
        initial_amplification,
        final_amplification,
        initial_block,
        final_block,
        current_block,
    )
    .to_string()
}

#[no_mangle]
pub fn Java_io_novafoundation_nova_hydra_1dx_1math_stableswap_StableSwapMathBridge_calculate_1shares<'a>(
    jni_env: JNIEnv<'a>,
    _: JClass,
    reserves: JString,
    assets: JString,
    amplification: JString,
    share_issuance: JString,
    fee: JString,
    pegs: JString,
) -> JString<'a> {
    let reserves = get_str(&jni_env, reserves);
    let assets = get_str(&jni_env, assets);
    let amplification = get_str(&jni_env, amplification);
    let share_issuance = get_str(&jni_env, share_issuance);
    let fee = get_str(&jni_env, fee);
    let pegs = get_str(&jni_env, pegs);

    let result = calculate_shares(reserves, assets, amplification, share_issuance, fee, pegs);

    jni_env.new_string(result).unwrap()
}

fn calculate_shares(
    reserves: String,
    assets: String,
    amplification: String,
    share_issuance: String,
    fee: String,
    pegs: String,
) -> String {
    let reserves: serde_json::Result<Vec<AssetBalance>> = serde_json::from_str(&reserves);
    if reserves.is_err() {
        return error();
    }
    let mut reserves = reserves.unwrap();
    reserves.sort_by_key(|v| v.asset_id);

    let assets: serde_json::Result<Vec<AssetAmount>> = serde_json::from_str(&assets);
    if assets.is_err() {
        return error();
    }
    let assets = assets.unwrap();
    if assets.len() > reserves.len() {
        return error();
    }

    let mut updated_reserves = reserves.clone();

    let mut liquidity: HashMap<u32, u128> = HashMap::new();
    for a in assets.iter() {
        let r = liquidity.insert(a.asset_id, a.amount);
        if r.is_some() {
            return error();
        }
    }
    for reserve in updated_reserves.iter_mut() {
        if let Some(v) = liquidity.get(&reserve.asset_id) {
            reserve.amount += v;
        }
    }
    let balances: Vec<AssetReserve> = reserves.iter().map(|v| v.into()).collect();
    let updated_balances: Vec<AssetReserve> = updated_reserves.iter().map(|v| v.into()).collect();
    let amplification = parse_into!(u128, amplification);
    let issuance = parse_into!(u128, share_issuance);
    let fee = Permill::from_float(parse_into!(f64, fee));

    let pairs = match parse_pairs(&pegs) {
        Some(p) => p,
        None => return error(),
    };

    let result = hydra_dx_math::stableswap::calculate_shares::<D_ITERATIONS>(
        &balances,
        &updated_balances,
        amplification,
        issuance,
        fee,
        &pairs,
    );

    if let Some(r) = result {
        r.0.to_string()
    } else {
        error()
    }
}

#[no_mangle]
pub fn Java_io_novafoundation_nova_hydra_1dx_1math_stableswap_StableSwapMathBridge_calculate_1shares_1for_1amount<'a>(
    jni_env: JNIEnv<'a>,
    _: JClass,
    reserves: JString,
    asset_in: jint,
    amount: JString,
    amplification: JString,
    share_issuance: JString,
    fee: JString,
    pegs: JString,
) -> JString<'a> {
    let reserves = get_str(&jni_env, reserves);
    let asset_in = asset_in as u32;
    let amount = get_str(&jni_env, amount);
    let amplification = get_str(&jni_env, amplification);
    let share_issuance = get_str(&jni_env, share_issuance);
    let fee = get_str(&jni_env, fee);
    let pegs = get_str(&jni_env, pegs);

    let result = calculate_shares_for_amount(
        reserves,
        asset_in,
        amount,
        amplification,
        share_issuance,
        fee,
        pegs,
    );

    jni_env.new_string(result).unwrap()
}

fn calculate_shares_for_amount(
    reserves: String,
    asset_in: u32,
    amount: String,
    amplification: String,
    share_issuance: String,
    fee: String,
    pegs: String,
) -> String {
    let reserves: serde_json::Result<Vec<AssetBalance>> = serde_json::from_str(&reserves);
    if reserves.is_err() {
        return error();
    }
    let mut reserves = reserves.unwrap();
    reserves.sort_by_key(|v| v.asset_id);
    let idx_in = reserves.iter().position(|v| v.asset_id == asset_in);
    if idx_in.is_none() {
        return error();
    }
    let amount_in = parse_into!(u128, amount);
    let balances: Vec<AssetReserve> = reserves.iter().map(|v| v.into()).collect();
    let amplification = parse_into!(u128, amplification);
    let issuance = parse_into!(u128, share_issuance);
    let fee = Permill::from_float(parse_into!(f64, fee));

    // Parse 7th param
    let pairs = match parse_pairs(&pegs) {
        Some(p) => p,
        None => return error(),
    };

    let result = hydra_dx_math::stableswap::calculate_shares_for_amount::<D_ITERATIONS>(
        &balances,
        idx_in.unwrap(),
        amount_in,
        amplification,
        issuance,
        fee,
        &pairs,
    );

    if let Some(r) = result {
        r.0.to_string()
    } else {
        error()
    }
}

#[no_mangle]
pub fn Java_io_novafoundation_nova_hydra_1dx_1math_stableswap_StableSwapMathBridge_calculate_1add_1one_1asset<'a>(
    jni_env: JNIEnv<'a>,
    _: JClass,
    reserves: JString,
    shares: JString,
    asset_in: jint,
    amplification: JString,
    share_issuance: JString,
    fee: JString,
    pegs: JString,
) -> JString<'a> {
    let reserves = get_str(&jni_env, reserves);
    let shares = get_str(&jni_env, shares);
    let asset_in = asset_in as u32;
    let amplification = get_str(&jni_env, amplification);
    let share_issuance = get_str(&jni_env, share_issuance);
    let fee = get_str(&jni_env, fee);
    let pegs = get_str(&jni_env, pegs);

    let result = calculate_add_one_asset(
        reserves,
        shares,
        asset_in,
        amplification,
        share_issuance,
        fee,
        pegs,
    );

    jni_env.new_string(result).unwrap()
}

fn calculate_add_one_asset(
    reserves: String,
    shares: String,
    asset_in: u32,
    amplification: String,
    share_issuance: String,
    fee: String,
    pegs: String,
) -> String {
    let reserves: serde_json::Result<Vec<AssetBalance>> = serde_json::from_str(&reserves);
    if reserves.is_err() {
        return error();
    }
    let mut reserves = reserves.unwrap();
    reserves.sort_by_key(|v| v.asset_id);
    let idx_in = reserves.iter().position(|v| v.asset_id == asset_in);
    if idx_in.is_none() {
        return error();
    }

    let balances: Vec<AssetReserve> = reserves.iter().map(|v| v.into()).collect();
    let shares = parse_into!(u128, shares);
    let amplification = parse_into!(u128, amplification);
    let issuance = parse_into!(u128, share_issuance);
    let fee = Permill::from_float(parse_into!(f64, fee));

    let pairs = match parse_pairs(&pegs) {
        Some(p) => p,
        None => return error(),
    };

    let result = hydra_dx_math::stableswap::calculate_add_one_asset::<D_ITERATIONS, Y_ITERATIONS>(
            &balances,
            shares,
            idx_in.unwrap(),
            issuance,
            amplification,
            fee,
            &pairs,
        );

    if let Some(r) = result {
        r.0.to_string()
    } else {
        error()
    }
}

#[no_mangle]
pub fn Java_io_novafoundation_nova_hydra_1dx_1math_stableswap_StableSwapMathBridge_calculate_1liquidity_1out_1one_1asset<'a>(
    jni_env: JNIEnv<'a>,
    _: JClass,
    reserves: JString,
    shares: JString,
    asset_out: jint,
    amplification: JString,
    share_issuance: JString,
    withdraw_fee: JString,
    pegs: JString,
) -> JString<'a> {
    let reserves = get_str(&jni_env, reserves);
    let shares = get_str(&jni_env, shares);
    let asset_out = asset_out as u32;
    let amplification = get_str(&jni_env, amplification);
    let share_issuance = get_str(&jni_env, share_issuance);
    let withdraw_fee = get_str(&jni_env, withdraw_fee);
    let pegs = get_str(&jni_env, pegs);

    let result = calculate_liquidity_out_one_asset(
        reserves,
        shares,
        asset_out,
        amplification,
        share_issuance,
        withdraw_fee,
        pegs,
    );

    jni_env.new_string(result).unwrap()
}

fn calculate_liquidity_out_one_asset(
    reserves: String,
    shares: String,
    asset_out: u32,
    amplification: String,
    share_issuance: String,
    withdraw_fee: String,
    pegs: String,
) -> String {
    let reserves: serde_json::Result<Vec<AssetBalance>> = serde_json::from_str(&reserves);
    if reserves.is_err() {
        return error();
    }
    let mut reserves = reserves.unwrap();
    reserves.sort_by_key(|v| v.asset_id);

    let idx_out = reserves.iter().position(|v| v.asset_id == asset_out);
    if idx_out.is_none() {
        println!("idx_out error");
        return error();
    }

    let shares_out = parse_into!(u128, shares);
    let amplification = parse_into!(u128, amplification);
    let issuance = parse_into!(u128, share_issuance);
    let fee = Permill::from_float(parse_into!(f64, withdraw_fee));

    let balances: Vec<AssetReserve> = reserves.iter().map(|v| v.into()).collect();

    let pairs = match parse_pairs(&pegs) {
        Some(p) => p,
        None => { println!("parse_pairs error");  return error();         },
    };

    let result = hydra_dx_math::stableswap::calculate_withdraw_one_asset::<D_ITERATIONS, Y_ITERATIONS>(
            &balances,
            shares_out,
            idx_out.unwrap(),
            issuance,
            amplification,
            fee,
            &pairs,
        );

    if let Some(r) = result {
        r.0.to_string()
    } else {
    println!("final result error");
        error()
    }
}

/* ---------------- XYK ---------------------- */

#[no_mangle]
pub fn Java_io_novafoundation_nova_hydra_1dx_1math_xyk_HYKSwapMathBridge_calculate_1out_1given_1in<'a>(
    jni_env: JNIEnv<'a>,
    _: JClass,
    balance_in: JString,
    balance_out: JString,
    amount_in: JString,
) -> JString<'a> {
    let balance_in: String = get_str(&jni_env, balance_in);
    let balance_out: String = get_str(&jni_env, balance_out);
    let amount_in: String = get_str(&jni_env, amount_in);

    let out = xyk_calculate_out_given_in(balance_in, balance_out, amount_in);

    jni_env.new_string(out).unwrap()
}

fn xyk_calculate_out_given_in(balance_in: String, balance_out: String, amount_in: String) -> String {
    let balance_in = parse_into!(u128, balance_in);
    let balance_out = parse_into!(u128, balance_out);
    let amount_in = parse_into!(u128, amount_in);

    let result = hydra_dx_math::xyk::calculate_out_given_in(balance_in, balance_out, amount_in);

    if let Ok(r) = result {
        r.to_string()
    } else {
        error()
    }
}

#[no_mangle]
pub fn Java_io_novafoundation_nova_hydra_1dx_1math_xyk_HYKSwapMathBridge_calculate_1in_1given_1out<'a>(
    jni_env: JNIEnv<'a>,
    _: JClass,
    balance_in: JString,
    balance_out: JString,
    amount_in: JString,
) -> JString<'a> {
    let balance_in: String = get_str(&jni_env, balance_in);
    let balance_out: String = get_str(&jni_env, balance_out);
    let amount_in: String = get_str(&jni_env, amount_in);

    let out = xyk_calculate_in_given_out(balance_in, balance_out, amount_in);

    jni_env.new_string(out).unwrap()
}

fn xyk_calculate_in_given_out(balance_in: String, balance_out: String, amount_out: String) -> String {
    let balance_in = parse_into!(u128, balance_in);
    let balance_out = parse_into!(u128, balance_out);
    let amount_out = parse_into!(u128, amount_out);

    let result = hydra_dx_math::xyk::calculate_in_given_out(balance_out, balance_in, amount_out);

    if let Ok(r) = result {
        r.to_string()
    } else {
        error()
    }
}

#[no_mangle]
pub fn Java_io_novafoundation_nova_hydra_1dx_1math_xyk_HYKSwapMathBridge_calculate_1pool_1trade_1fee<'a>(
    jni_env: JNIEnv<'a>,
    _: JClass,
    amount: JString,
    fee_nominator: JString,
    fee_denominator: JString,
) -> JString<'a> {
    let amount: String = get_str(&jni_env, amount);
    let fee_nominator: String = get_str(&jni_env, fee_nominator);
    let fee_denominator: String = get_str(&jni_env, fee_denominator);

    let out = calculate_pool_trade_fee(amount, fee_nominator, fee_denominator);

    jni_env.new_string(out).unwrap()
}

fn calculate_pool_trade_fee(amount: String, fee_nominator: String, fee_denominator: String) -> String {
    let amount = parse_into!(u128, amount);
    let fee_nominator = parse_into!(u32, fee_nominator);
    let fee_denominator = parse_into!(u32, fee_denominator);

    let result = hydra_dx_math::fee::calculate_pool_trade_fee(amount, (fee_nominator, fee_denominator));

    if let Some(r) = result {
        r.to_string()
    } else {
        error()
    }
}

/* ---------------- OMNIPOOL ---------------------- */

use hydra_dx_math::omnipool::types::{AssetReserveState, BalanceUpdate, TradeSlipFees, SignedBalance};

fn build_asset_reserve_state(reserve: u128, hub_reserve: u128, shares: u128) -> AssetReserveState<u128> {
    AssetReserveState {
        reserve,
        hub_reserve,
        shares,
        protocol_shares: 0,
    }
}

fn build_slip_fees(
    asset_in_hub_reserve: u128,
    asset_out_hub_reserve: u128,
    max_slip_fee: Permill,
) -> Option<TradeSlipFees> {
    if max_slip_fee == Permill::zero() {
        None
    } else {
        Some(TradeSlipFees {
            asset_in_hub_reserve,
            asset_in_delta: SignedBalance::Positive(0),
            asset_out_hub_reserve,
            asset_out_delta: SignedBalance::Positive(0),
            max_slip_fee,
        })
    }
}

#[no_mangle]
pub fn Java_io_novafoundation_nova_hydra_1dx_1math_omnipool_OmniPoolMathBridge_calculate_1out_1given_1in<'a>(
    jni_env: JNIEnv<'a>,
    _: JClass,
    asset_in_reserve: JString,
    asset_in_hub_reserve: JString,
    asset_in_shares: JString,
    asset_out_reserve: JString,
    asset_out_hub_reserve: JString,
    asset_out_shares: JString,
    amount_in: JString,
    asset_fee: JString,
    protocol_fee: JString,
    max_slip_fee: JString,
) -> JString<'a> {
    let asset_in_reserve = get_str(&jni_env, asset_in_reserve);
    let asset_in_hub_reserve = get_str(&jni_env, asset_in_hub_reserve);
    let asset_in_shares = get_str(&jni_env, asset_in_shares);
    let asset_out_reserve = get_str(&jni_env, asset_out_reserve);
    let asset_out_hub_reserve = get_str(&jni_env, asset_out_hub_reserve);
    let asset_out_shares = get_str(&jni_env, asset_out_shares);
    let amount_in = get_str(&jni_env, amount_in);
    let asset_fee = get_str(&jni_env, asset_fee);
    let protocol_fee = get_str(&jni_env, protocol_fee);
    let max_slip_fee = get_str(&jni_env, max_slip_fee);

    let out = omnipool_calculate_out_given_in(
        asset_in_reserve, asset_in_hub_reserve, asset_in_shares,
        asset_out_reserve, asset_out_hub_reserve, asset_out_shares,
        amount_in, asset_fee, protocol_fee, max_slip_fee,
    );

    jni_env.new_string(out).unwrap()
}

fn omnipool_calculate_out_given_in(
    asset_in_reserve: String,
    asset_in_hub_reserve: String,
    asset_in_shares: String,
    asset_out_reserve: String,
    asset_out_hub_reserve: String,
    asset_out_shares: String,
    amount_in: String,
    asset_fee: String,
    protocol_fee: String,
    max_slip_fee: String,
) -> String {
    let asset_in_reserve = parse_into!(u128, asset_in_reserve);
    let asset_in_hub_reserve = parse_into!(u128, asset_in_hub_reserve);
    let asset_in_shares = parse_into!(u128, asset_in_shares);
    let asset_out_reserve = parse_into!(u128, asset_out_reserve);
    let asset_out_hub_reserve = parse_into!(u128, asset_out_hub_reserve);
    let asset_out_shares = parse_into!(u128, asset_out_shares);
    let amount = parse_into!(u128, amount_in);
    let asset_fee = Permill::from_float(parse_into!(f64, asset_fee));
    let protocol_fee = Permill::from_float(parse_into!(f64, protocol_fee));
    let max_slip_fee = Permill::from_float(parse_into!(f64, max_slip_fee));

    let asset_in = build_asset_reserve_state(asset_in_reserve, asset_in_hub_reserve, asset_in_shares);
    let asset_out = build_asset_reserve_state(asset_out_reserve, asset_out_hub_reserve, asset_out_shares);
    let slip = build_slip_fees(asset_in_hub_reserve, asset_out_hub_reserve, max_slip_fee);

    let result = hydra_dx_math::omnipool::calculate_sell_state_changes(
        &asset_in,
        &asset_out,
        amount,
        asset_fee,
        protocol_fee,
        Permill::zero(),
        slip.as_ref(),
    );

    match result {
        Some(state) => {
            match state.asset_out.delta_reserve {
                BalanceUpdate::Increase(v) => v.to_string(),
                BalanceUpdate::Decrease(v) => v.to_string(),
            }
        }
        None => error(),
    }
}

#[no_mangle]
pub fn Java_io_novafoundation_nova_hydra_1dx_1math_omnipool_OmniPoolMathBridge_calculate_1in_1given_1out<'a>(
    jni_env: JNIEnv<'a>,
    _: JClass,
    asset_in_reserve: JString,
    asset_in_hub_reserve: JString,
    asset_in_shares: JString,
    asset_out_reserve: JString,
    asset_out_hub_reserve: JString,
    asset_out_shares: JString,
    amount_out: JString,
    asset_fee: JString,
    protocol_fee: JString,
    max_slip_fee: JString,
) -> JString<'a> {
    let asset_in_reserve = get_str(&jni_env, asset_in_reserve);
    let asset_in_hub_reserve = get_str(&jni_env, asset_in_hub_reserve);
    let asset_in_shares = get_str(&jni_env, asset_in_shares);
    let asset_out_reserve = get_str(&jni_env, asset_out_reserve);
    let asset_out_hub_reserve = get_str(&jni_env, asset_out_hub_reserve);
    let asset_out_shares = get_str(&jni_env, asset_out_shares);
    let amount_out = get_str(&jni_env, amount_out);
    let asset_fee = get_str(&jni_env, asset_fee);
    let protocol_fee = get_str(&jni_env, protocol_fee);
    let max_slip_fee = get_str(&jni_env, max_slip_fee);

    let out = omnipool_calculate_in_given_out(
        asset_in_reserve, asset_in_hub_reserve, asset_in_shares,
        asset_out_reserve, asset_out_hub_reserve, asset_out_shares,
        amount_out, asset_fee, protocol_fee, max_slip_fee,
    );

    jni_env.new_string(out).unwrap()
}

fn omnipool_calculate_in_given_out(
    asset_in_reserve: String,
    asset_in_hub_reserve: String,
    asset_in_shares: String,
    asset_out_reserve: String,
    asset_out_hub_reserve: String,
    asset_out_shares: String,
    amount_out: String,
    asset_fee: String,
    protocol_fee: String,
    max_slip_fee: String,
) -> String {
    let asset_in_reserve = parse_into!(u128, asset_in_reserve);
    let asset_in_hub_reserve = parse_into!(u128, asset_in_hub_reserve);
    let asset_in_shares = parse_into!(u128, asset_in_shares);
    let asset_out_reserve = parse_into!(u128, asset_out_reserve);
    let asset_out_hub_reserve = parse_into!(u128, asset_out_hub_reserve);
    let asset_out_shares = parse_into!(u128, asset_out_shares);
    let amount = parse_into!(u128, amount_out);
    let asset_fee = Permill::from_float(parse_into!(f64, asset_fee));
    let protocol_fee = Permill::from_float(parse_into!(f64, protocol_fee));
    let max_slip_fee = Permill::from_float(parse_into!(f64, max_slip_fee));

    let asset_in = build_asset_reserve_state(asset_in_reserve, asset_in_hub_reserve, asset_in_shares);
    let asset_out = build_asset_reserve_state(asset_out_reserve, asset_out_hub_reserve, asset_out_shares);
    let slip = build_slip_fees(asset_in_hub_reserve, asset_out_hub_reserve, max_slip_fee);

    let result = hydra_dx_math::omnipool::calculate_buy_state_changes(
        &asset_in,
        &asset_out,
        amount,
        asset_fee,
        protocol_fee,
        Permill::zero(),
        slip.as_ref(),
    );

    match result {
        Some(state) => {
            match state.asset_in.delta_reserve {
                BalanceUpdate::Increase(v) => v.to_string(),
                BalanceUpdate::Decrease(v) => v.to_string(),
            }
        }
        None => error(),
    }
}

#[cfg(test)]
mod omnipool_tests {
    use super::*;

    const UNIT: u128 = 1_000_000_000_000;

    fn to_unit(amount: u128) -> String {
        (amount * UNIT).to_string()
    }

    #[test]
    fn sell_should_work_with_no_fees() {
        let result = omnipool_calculate_out_given_in(
            to_unit(10_000_000),  // assetInReserve
            to_unit(20_000_000),  // assetInHubReserve
            to_unit(10_000_000),  // assetInShares
            to_unit(5_000_000),   // assetOutReserve
            to_unit(5_000_000),   // assetOutHubReserve
            to_unit(20_000_000),  // assetOutShares
            to_unit(4_000_000),   // amountIn
            "0".to_string(),      // assetFee
            "0".to_string(),      // protocolFee
            "0".to_string(),      // maxSlipFee
        );

        assert_ne!(result, "-1", "Should not return error");
        let amount_out: u128 = result.parse().expect("Should be valid number");
        assert!(amount_out > 0, "Amount out should be positive");
    }

    #[test]
    fn sell_with_fees_should_reduce_output() {
        let no_fee = omnipool_calculate_out_given_in(
            to_unit(10_000_000),
            to_unit(20_000_000),
            to_unit(10_000_000),
            to_unit(5_000_000),
            to_unit(5_000_000),
            to_unit(20_000_000),
            to_unit(4_000_000),
            "0".to_string(),
            "0".to_string(),
            "0".to_string(),
        ).parse::<u128>().unwrap();

        let with_fee = omnipool_calculate_out_given_in(
            to_unit(10_000_000),
            to_unit(20_000_000),
            to_unit(10_000_000),
            to_unit(5_000_000),
            to_unit(5_000_000),
            to_unit(20_000_000),
            to_unit(4_000_000),
            "0.01".to_string(),   // 1% asset fee
            "0".to_string(),
            "0".to_string(),
        ).parse::<u128>().unwrap();

        assert!(with_fee < no_fee, "With fee ({}) should be less than no fee ({})", with_fee, no_fee);
    }

    #[test]
    fn sell_with_slip_fee_should_reduce_output() {
        let no_slip = omnipool_calculate_out_given_in(
            to_unit(10_000_000),
            to_unit(10_000_000),
            to_unit(10_000_000),
            to_unit(500_000),
            to_unit(5_000_000),
            to_unit(500_000),
            to_unit(100_000),
            "0.0025".to_string(),
            "0.0005".to_string(),
            "0".to_string(),      // no slip fee
        );

        let with_slip = omnipool_calculate_out_given_in(
            to_unit(10_000_000),
            to_unit(10_000_000),
            to_unit(10_000_000),
            to_unit(500_000),
            to_unit(5_000_000),
            to_unit(500_000),
            to_unit(100_000),
            "0.0025".to_string(),
            "0.0005".to_string(),
            "0.05".to_string(),   // 5% max slip fee
        );

        assert_ne!(no_slip, "-1");
        assert_ne!(with_slip, "-1");

        let no_slip_out: u128 = no_slip.parse().unwrap();
        let with_slip_out: u128 = with_slip.parse().unwrap();

        assert!(with_slip_out < no_slip_out,
            "Slip fee should reduce output: {} < {}", with_slip_out, no_slip_out);
    }

    #[test]
    fn buy_should_work_with_no_fees() {
        let result = omnipool_calculate_in_given_out(
            to_unit(10_000_000),
            to_unit(20_000_000),
            to_unit(10_000_000),
            to_unit(5_000_000),
            to_unit(5_000_000),
            to_unit(20_000_000),
            to_unit(1_000_000),
            "0".to_string(),
            "0".to_string(),
            "0".to_string(),
        );

        assert_ne!(result, "-1", "Should not return error");
        let amount_in: u128 = result.parse().expect("Should be valid number");
        assert!(amount_in > 0, "Amount in should be positive");
    }

    #[test]
    fn buy_with_slip_fee_should_increase_cost() {
        let no_slip = omnipool_calculate_in_given_out(
            to_unit(10_000_000),
            to_unit(10_000_000),
            to_unit(10_000_000),
            to_unit(500_000),
            to_unit(5_000_000),
            to_unit(500_000),
            to_unit(1_000),
            "0.0025".to_string(),
            "0.0005".to_string(),
            "0".to_string(),
        );

        let with_slip = omnipool_calculate_in_given_out(
            to_unit(10_000_000),
            to_unit(10_000_000),
            to_unit(10_000_000),
            to_unit(500_000),
            to_unit(5_000_000),
            to_unit(500_000),
            to_unit(1_000),
            "0.0025".to_string(),
            "0.0005".to_string(),
            "0.05".to_string(),
        );

        assert_ne!(no_slip, "-1");
        assert_ne!(with_slip, "-1");

        let no_slip_in: u128 = no_slip.parse().unwrap();
        let with_slip_in: u128 = with_slip.parse().unwrap();

        assert!(with_slip_in > no_slip_in,
            "Slip fee should increase cost: {} > {}", with_slip_in, no_slip_in);
    }

    #[test]
    fn sell_returns_error_on_invalid_input() {
        let result = omnipool_calculate_out_given_in(
            "invalid".to_string(),
            to_unit(20_000_000),
            to_unit(10_000_000),
            to_unit(5_000_000),
            to_unit(5_000_000),
            to_unit(20_000_000),
            to_unit(4_000_000),
            "0".to_string(),
            "0".to_string(),
            "0".to_string(),
        );

        assert_eq!(result, "-1", "Should return error for invalid input");
    }

    #[test]
    fn buy_returns_error_on_invalid_input() {
        let result = omnipool_calculate_in_given_out(
            to_unit(10_000_000),
            to_unit(20_000_000),
            to_unit(10_000_000),
            to_unit(5_000_000),
            to_unit(5_000_000),
            to_unit(20_000_000),
            "invalid".to_string(),
            "0".to_string(),
            "0".to_string(),
            "0".to_string(),
        );

        assert_eq!(result, "-1", "Should return error for invalid input");
    }

    #[test]
    fn sell_with_zero_slip_fee_should_match_no_slip() {
        let no_slip = omnipool_calculate_out_given_in(
            to_unit(10_000_000),
            to_unit(10_000_000),
            to_unit(10_000_000),
            to_unit(500_000),
            to_unit(5_000_000),
            to_unit(500_000),
            to_unit(100_000),
            "0.0025".to_string(),
            "0.0005".to_string(),
            "0".to_string(),
        );

        let explicit_zero = omnipool_calculate_out_given_in(
            to_unit(10_000_000),
            to_unit(10_000_000),
            to_unit(10_000_000),
            to_unit(500_000),
            to_unit(5_000_000),
            to_unit(500_000),
            to_unit(100_000),
            "0.0025".to_string(),
            "0.0005".to_string(),
            "0".to_string(),
        );

        assert_eq!(no_slip, explicit_zero, "Zero slip and no slip should match");
    }

}