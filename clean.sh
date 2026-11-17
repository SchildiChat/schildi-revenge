#!/bin/bash

set -e

pushd matrix-rust-sdk
cargo clean
popd

pushd matrix-rust-rtc
cargo clean
popd

./gradlew clean
