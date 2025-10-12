### Nova Wallet Android - Next gen mobile app for Polkadot & Kusama ecosystem

[![](https://img.shields.io/twitter/follow/NovaWalletApp?label=Follow&style=social)](https://twitter.com/NovaWalletApp)

![logo](/docs/Nova_GitHub.png)

## About
Next gen application for Polkadot & Kusama ecosystem, transparent & community-oriented, focused on convenient UX/UI, fast performance & security.
Nova Wallet aims to provide as many Polkadot ecosystem features as possible in a form of mobile app, unbiased to any network & without any restrictions/limits to the users.
Developed by the former Fearless Wallet team & based on open-source work under Apache 2.0 license.

### Build instructions

#### Clone repo

```
git clone git@github.com:novasamatech/nova-wallet-android.git
```

#### Install NDK

Install NDK version `26.1.10909125` from SDK Manager by going to Tools -> SDK Manager -> Sdk Tools -> NDK (Side by Side)

#### Install Rust

Install Rust by following [official instructions](https://www.rust-lang.org/tools/install). Use "Using rustup" option. On MacOS you can also install rust with [brew](https://formulae.brew.sh/formula/rust)

Run the following commands to add Android build targets to rustup:

```
rustup target add armv7-linux-androideabi
rustup target add i686-linux-android
rustup target add x86_64-linux-android
rustup target add aarch64-linux-android
```

#### Update local.properties

Add the following lines to your local.properties

```
ACALA_PROD_AUTH_TOKEN=mock
ACALA_TEST_AUTH_TOKEN=mock
CI_KEYSTORE_KEY_ALIAS=mock
CI_KEYSTORE_KEY_PASS=mock
CI_KEYSTORE_PASS=mock
DEBUG_GOOGLE_OAUTH_ID=mock
RELEASE_GOOGLE_OAUTH_ID=mock
DWELLIR_API_KEY=mock
EHTERSCAN_API_KEY_ETHEREUM=mock
EHTERSCAN_API_KEY_MOONBEAM=mock
EHTERSCAN_API_KEY_MOONRIVER=mock
INFURA_API_KEY=mock
MERCURYO_PRODUCTION_SECRET=mock
MERCURYO_TEST_SECRET=mock
MOONBEAM_PROD_AUTH_TOKEN=mock
MOONBEAM_TEST_AUTH_TOKEN=mock
MOONPAY_PRODUCTION_SECRET=mock
MOONPAY_TEST_SECRET=mock
WALLET_CONNECT_PROJECT_ID=mock
```
Note, that Firebase and Google related features (Notifications, Cloud Backups) won't work properly with this setup.

If you face `Secret X is not found` error, it means that README is not up-to-date. Add `X=mock` line to local.properties as well

#### Setup build type

In Nova Wallet, `debug` build type uses fixed key, not publicly shared keystore to fix fingerprint to make Google-related features work
To use your local debug keystore switch to `debugLocal` build type.

#### QA Tools

This project is tested with BrowserStack. We use this testing automation tool to maintain a high level of quality for each new version of the application.

## License
Nova Wallet Android is available under the Apache 2.0 license. See the LICENSE file for more info.
© Novasama Technologies GmbH 2023
