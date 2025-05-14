#![allow(non_snake_case)]

extern crate core;
extern crate jni;
extern crate merlin;

use jni::JNIEnv;
use jni::objects::{JClass, JList, JObject};
use jni::sys::jbyteArray;
use jni::errors::{Result as JniResult, Error as JniError, ErrorKind};
use merlin::Transcript;
use std::fmt::Write;
use std::num::ParseIntError;
use std::ptr;
use schnorrkel::{
    derive::{CHAIN_CODE_LENGTH, ChainCode, Derivation}, ExpansionMode, Keypair, MiniSecretKey, PublicKey,
    SecretKey, Signature};

macro_rules! r#try_or_throw {
    ($jni_env: ident, $expr:expr, $ret: expr) => {
        match $expr {
            JniResult::Ok(val) => val,
            JniResult::Err(err) => {
                $jni_env.throw_new("java/lang/Exception", err.description()).unwrap();
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
    }
}

#[no_mangle]
pub unsafe extern "system" fn Java_io_novafoundation_nova_merlin_MerlinCrypto_generateTranscript
(
    jni_env: JNIEnv,
    _: JClass,
    public_key: jbyteArray,
    secret: jbyteArray,
    message: jbyteArray,
) -> jbyteArray {

    let public_key_vec = try_or_throw_null!(jni_env, jni_env.convert_byte_array(public_key));
    let secret_vec = try_or_throw_null!(jni_env, jni_env.convert_byte_array(secret));

    let message_vec = try_or_throw_null!(jni_env, jni_env.convert_byte_array(message));

    let secret = try_or_throw_null!(jni_env, create_secret(secret_vec.as_slice()));
    let public = try_or_throw_null!(jni_env, create_public(public_key_vec.as_slice()));

    let mut transcript = Transcript::new(b"raise-auth");
    transcript.append_message(b"challenge-nonce", &message_vec[..]);

    let signature = secret.sign(transcript, &public);

    try_or_throw_null!(jni_env, jni_env.byte_array_from_slice(signature.to_bytes().as_ref()))
}

/// PublicKey helper
fn create_public(public: &[u8]) -> JniResult<PublicKey> {
    match PublicKey::from_bytes(public) {
        Ok(public) => return JniResult::Ok(public),
        Err(_) => return JniResult::Err(
            JniError::from_kind(ErrorKind::Msg("Provided public key is invalid.".to_string()))),
    }
}

/// SecretKey helper
fn create_secret(secret: &[u8]) -> JniResult<SecretKey> {
    match SecretKey::from_bytes(secret) {
        Ok(secret) => return JniResult::Ok(secret),
        Err(_) => return JniResult::Err(
            JniError::from_kind(ErrorKind::Msg("Provided private key is invalid.".to_string()))),
    }
}
