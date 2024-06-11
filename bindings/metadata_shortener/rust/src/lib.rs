#![allow(non_snake_case)]

extern crate core;
extern crate jni;
extern crate merkleized_metadata;

use codec::{Decode, Encode};
use frame_metadata::{RuntimeMetadataPrefixed, OpaqueMetadata, RuntimeMetadata};
use jni::{errors::Result as JniResult, sys::jint};
use jni::objects::{JClass, JString};
use jni::sys::jbyteArray;
use jni::JNIEnv;
use merkleized_metadata::{generate_metadata_digest, generate_proof_for_extrinsic_parts, ExtraInfo, SignedExtrinsicData, FrameMetadataPrepared, Proof, ExtrinsicMetadata};
use std::ptr;

#[derive(Encode)]
pub struct MetadataProof {
    proof: Proof,
    extrinsic: ExtrinsicMetadata,
    extra_info: ExtraInfo,
}

macro_rules! r#try_or_throw {
    ($jni_env: ident, $expr:expr, $ret: expr) => {
        match $expr {
            JniResult::Ok(val) => val,
            JniResult::Err(err) => {
                $jni_env
                    .throw_new("java/lang/Exception", err.description())
                    .unwrap();
                return $ret;
            }
        }
    };
    ($expr:expr,) => {
        $crate::r#try!($expr)
    };
}

macro_rules! r#try_or_throw_null {
    ($jni_env: ident, $expr:expr) => {
        try_or_throw!($jni_env, $expr, ptr::null_mut())
    };
}

#[no_mangle]
fn Java_io_novafoundation_nova_metadata_1shortener_MetadataShortener_generate_1extrinsic_1proof(
    jni_env: JNIEnv,
    _: JClass,
    call: jbyteArray,
    signed_extras: jbyteArray,
    additional_signed: jbyteArray,
    metadata: jbyteArray,
    spec_version: jint,
    spec_name: JString,
    base58_prefix: jint,
    decimals: jint,
    token_symbol: JString,
) -> jbyteArray {
    let Some(metadata) = decode_metadata(&jni_env, metadata) else {
        return ptr::null_mut();
    };

    let included_in_extrinsic =
        try_or_throw_null!(jni_env, jni_env.convert_byte_array(signed_extras));
    let included_in_signed_data =
        try_or_throw_null!(jni_env, jni_env.convert_byte_array(additional_signed));

    let call_vec = try_or_throw_null!(jni_env, jni_env.convert_byte_array(call));

    let signed_ext_data = SignedExtrinsicData {
        included_in_extrinsic: included_in_extrinsic.as_slice(),
        included_in_signed_data: included_in_signed_data.as_slice(),
    };

    let spec_version = spec_version as u32;
    let spec_name = try_or_throw_null!(jni_env, jni_env.get_string(spec_name));
    let base58_prefix = base58_prefix as u16;
    let decimals = decimals as u8;
    let token_symbol = try_or_throw_null!(jni_env, jni_env.get_string(token_symbol));

    let extra_info = ExtraInfo {
        spec_version: spec_version,
        spec_name: spec_name.into(),
        base58_prefix: base58_prefix,
        decimals: decimals,
        token_symbol: token_symbol.into(),
    };

     let extrinsic_metadata = FrameMetadataPrepared::prepare(&metadata)
        .unwrap()
        .as_type_information()
        .unwrap()
        .extrinsic_metadata;

    let Ok(registry_proof) =
        generate_proof_for_extrinsic_parts(call_vec.as_slice(), Some(signed_ext_data), &metadata)
    else {
        jni_env
            .throw_new("java/lang/Exception", "Failed to construct proof")
            .unwrap();
        return ptr::null_mut();
    };

    let meta_proof = MetadataProof {
                 proof: registry_proof,
                 extrinsic: extrinsic_metadata,
                 extra_info: extra_info,
             };

    let prood_encoded = meta_proof.encode();

    try_or_throw_null!(
        jni_env,
        jni_env.byte_array_from_slice(prood_encoded.as_slice().as_ref())
    )
}

#[no_mangle]
fn Java_io_novafoundation_nova_metadata_1shortener_MetadataShortener_generate_1metadata_1digest(
    jni_env: JNIEnv,
    _: JClass,
    metadata: jbyteArray,
    spec_version: jint,
    spec_name: JString,
    base58_prefix: jint,
    decimals: jint,
    token_symbol: JString,
) -> jbyteArray {
    let Some(metadata) = decode_metadata(&jni_env, metadata) else {
        return ptr::null_mut();
    };

    let spec_version = spec_version as u32;
    let spec_name = try_or_throw_null!(jni_env, jni_env.get_string(spec_name));
    let base58_prefix = base58_prefix as u16;
    let decimals = decimals as u8;
    let token_symbol = try_or_throw_null!(jni_env, jni_env.get_string(token_symbol));

    let extra_info = ExtraInfo {
        spec_version: spec_version,
        spec_name: spec_name.into(),
        base58_prefix: base58_prefix,
        decimals: decimals,
        token_symbol: token_symbol.into(),
    };

    let Ok(digest) = generate_metadata_digest(&metadata, extra_info) else {
        jni_env
            .throw_new("java/lang/Exception", "Failed to generate digest")
            .unwrap();
        return ptr::null_mut();
    };

    let digest_hash = digest.hash();

    try_or_throw_null!(
        jni_env,
        jni_env.byte_array_from_slice(digest_hash.as_slice().as_ref())
    )
}

fn decode_metadata(jni_env: &JNIEnv, metadata: jbyteArray) -> Option<RuntimeMetadata> {
    let metadata = try_or_throw!(jni_env, jni_env.convert_byte_array(metadata), None);

    let Some(metadata) = Option::<OpaqueMetadata>::decode(&mut &metadata[..])
        .ok()
		.flatten() else {
            jni_env
            .throw_new("java/lang/Exception", "Failed to decode opaque metadata")
            .unwrap();

            return None;
        };
    let metadata = metadata.0;

    let Ok(metadata) = RuntimeMetadataPrefixed::decode(&mut &metadata[..]) else {
        jni_env
            .throw_new("java/lang/Exception", "Failed to decode metadata")
            .unwrap();

        return None
    };

    Some(metadata.1)
}

// fn main() {
//     let signed_extras = "15000000";
//     let additional_signed = "104a0f001900000091b171bb158e2d3848fa23a9f1c25182fb8e20313b2c1eb49219da7a70ce90c36c9f8deedd0c7f1aae4d1900c88456f23d9c224934e6b8c57f750c8b146997c1";
//     let call = "050000010101010101010101010101010101010101010101010101010101010101010104";
//     let metadata = "Metadata goes here";

//     let signed_extras = hex2bytes(signed_extras).expect("Cant decode signed extras");
//     let additional_signed = hex2bytes(additional_signed).expect("Cant decode additional signed");
//     let call = hex2bytes(call).expect("Cant decode call");


//     let metadata = hex2bytes(metadata).expect("Cant decode metadata");
//  let metadata = Option::<OpaqueMetadata>::decode(&mut &metadata[..])
//         .expect("Failed to decode opaque metadata")
// 		.expect("Metadata V15 support is required.")
//         .0;
//     let metadata = RuntimeMetadataPrefixed::decode(&mut &metadata[..]).expect("Failed to decode metadata");

    
//     let signed_ext_data = SignedExtrinsicData {
//         included_in_extrinsic: signed_extras.as_slice(),
//         included_in_signed_data: additional_signed.as_slice(),
//     };

//     let proof = generate_proof_for_extrinsic_parts(call.as_slice(), Some(signed_ext_data), &metadata.1)
//     .expect("Failed to generate proof");
// }