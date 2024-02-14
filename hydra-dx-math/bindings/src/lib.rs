#![allow(non_snake_case)]

extern crate core;
extern crate hydra_dx_math;
extern crate jni;
extern crate serde;
extern crate sp_arithmetic;

use std::collections::HashMap;

use hydra_dx_math::stableswap::types::AssetReserve;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jint};
use serde::Deserialize;
use sp_arithmetic::Permill;

use serde_aux::prelude::*;
#[cfg(test)]
use sp_core::crypto::UncheckedFrom;
#[cfg(test)]
use sp_core::Hasher;
#[cfg(test)]
use sp_runtime::traits::IdentifyAccount;

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

fn get_str<'a>(jni: &'a JNIEnv<'a>, string: JString<'a>) -> String {
    return jni.get_string(string).unwrap().to_str().unwrap().to_string();
}


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
) -> JString<'a> {
    let reserves = get_str(&jni_env,reserves);
    let asset_in = asset_in as u32;
    let asset_out = asset_out as u32;
    let amount_in = get_str(&jni_env,amount_in);
    let amplification = get_str(&jni_env,amplification);
    let fee = get_str(&jni_env,fee);

    let out = calculate_out_given_in(reserves, asset_in, asset_out, amount_in, amplification, fee);

    return jni_env.new_string(out).unwrap()
}


fn calculate_out_given_in(
    reserves: String,
    asset_in: u32,
    asset_out: u32,
    amount_in: String,
    amplification: String,
    fee: String,
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

    let result = hydra_dx_math::stableswap::calculate_out_given_in_with_fee::<D_ITERATIONS, Y_ITERATIONS>(
        &balances,
        idx_in.unwrap(),
        idx_out.unwrap(),
        amount_in,
        amplification,
        fee,
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
) -> JString<'a> {
    let reserves = get_str(&jni_env,reserves);
    let asset_in = asset_in as u32;
    let asset_out = asset_out as u32;
    let amount_in = get_str(&jni_env,amount_in);
    let amplification = get_str(&jni_env,amplification);
    let fee = get_str(&jni_env,fee);

    let result = calculate_in_given_out(reserves, asset_in, asset_out, amount_in, amplification, fee);

    return jni_env.new_string(result).unwrap()
}

fn calculate_in_given_out(
    reserves: String,
    asset_in: u32,
    asset_out: u32,
    amount_out: String,
    amplification: String,
    fee: String,
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

    let result = hydra_dx_math::stableswap::calculate_in_given_out_with_fee::<D_ITERATIONS, Y_ITERATIONS>(
        &balances,
        idx_in.unwrap(),
        idx_out.unwrap(),
        amount_out,
        amplification,
        fee,
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
    let initial_amplification = get_str(&jni_env,initial_amplification);
    let final_amplification = get_str(&jni_env,final_amplification);
    let initial_block = get_str(&jni_env,initial_block);
    let final_block = get_str(&jni_env,final_block);
    let current_block = get_str(&jni_env,current_block);

    let result = calculate_amplification(initial_amplification, final_amplification, initial_block, final_block, current_block);

    return jni_env.new_string(result).unwrap()
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
) -> JString<'a> {
    let reserves = get_str(&jni_env,reserves);
    let assets = get_str(&jni_env,assets);
    let amplification = get_str(&jni_env,amplification);
    let share_issuance = get_str(&jni_env,share_issuance);
    let fee = get_str(&jni_env,fee);

    let result = calculate_shares(reserves, assets, amplification, share_issuance, fee);

    return jni_env.new_string(result).unwrap()
}

fn calculate_shares(
    reserves: String,
    assets: String,
    amplification: String,
    share_issuance: String,
    fee: String,
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

    let result = hydra_dx_math::stableswap::calculate_shares::<D_ITERATIONS>(
        &balances,
        &updated_balances,
        amplification,
        issuance,
        fee,
    );

    if let Some(r) = result {
        r.to_string()
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
) -> JString<'a> {
    let reserves = get_str(&jni_env,reserves);
    let asset_in = asset_in as u32;
    let amount = get_str(&jni_env,amount);
    let amplification = get_str(&jni_env,amplification);
    let share_issuance = get_str(&jni_env,share_issuance);
    let fee = get_str(&jni_env,fee);

    let result = calculate_shares_for_amount(reserves, asset_in, amount, amplification, share_issuance, fee);

    return jni_env.new_string(result).unwrap()
}


fn calculate_shares_for_amount(
    reserves: String,
    asset_in: u32,
    amount: String,
    amplification: String,
    share_issuance: String,
    fee: String,
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

    let result = hydra_dx_math::stableswap::calculate_shares_for_amount::<D_ITERATIONS>(
        &balances,
        idx_in.unwrap(),
        amount_in,
        amplification,
        issuance,
        fee,
    );

    if let Some(r) = result {
        r.to_string()
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
) -> JString<'a> {
    let reserves = get_str(&jni_env,reserves);
    let shares = get_str(&jni_env,shares);
    let asset_in = asset_in as u32;
    let amplification = get_str(&jni_env,amplification);
    let share_issuance = get_str(&jni_env,share_issuance);
    let fee = get_str(&jni_env,fee);

    let result = calculate_add_one_asset(reserves, shares, asset_in, amplification, share_issuance, fee);

    return jni_env.new_string(result).unwrap()
}


fn calculate_add_one_asset(
    reserves: String,
    shares: String,
    asset_in: u32,
    amplification: String,
    share_issuance: String,
    fee: String,
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

    let result = hydra_dx_math::stableswap::calculate_add_one_asset::<D_ITERATIONS, Y_ITERATIONS>(
        &balances,
        shares,
        idx_in.unwrap(),
        issuance,
        amplification,
        fee,
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
) -> JString<'a> {
    let reserves = get_str(&jni_env,reserves);
    let shares = get_str(&jni_env,shares);
    let asset_out = asset_out as u32;
    let amplification = get_str(&jni_env,amplification);
    let share_issuance = get_str(&jni_env,share_issuance);
    let withdraw_fee = get_str(&jni_env,withdraw_fee);

    let result = calculate_liquidity_out_one_asset(reserves, shares, asset_out, amplification, share_issuance, withdraw_fee);

    return jni_env.new_string(result).unwrap()
}


fn calculate_liquidity_out_one_asset(
    reserves: String,
    shares: String,
    asset_out: u32,
    amplification: String,
    share_issuance: String,
    withdraw_fee: String,
) -> String {
    let reserves: serde_json::Result<Vec<AssetBalance>> = serde_json::from_str(&reserves);
    if reserves.is_err() {
        return error();
    }
    let mut reserves = reserves.unwrap();
    reserves.sort_by_key(|v| v.asset_id);

    let idx_out = reserves.iter().position(|v| v.asset_id == asset_out);
    if idx_out.is_none() {
        return error();
    }

    let shares_out = parse_into!(u128, shares);
    let amplification = parse_into!(u128, amplification);
    let issuance = parse_into!(u128, share_issuance);
    let fee = Permill::from_float(parse_into!(f64, withdraw_fee));

    let balances: Vec<AssetReserve> = reserves.iter().map(|v| v.into()).collect();

    let result = hydra_dx_math::stableswap::calculate_withdraw_one_asset::<D_ITERATIONS, Y_ITERATIONS>(
        &balances,
        shares_out,
        idx_out.unwrap(),
        issuance,
        amplification,
        fee,
    );

    if let Some(r) = result {
        r.0.to_string()
    } else {
        error()
    }
}