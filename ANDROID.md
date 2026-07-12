# Experimental Android support

No, this is most likely not ready as daily driver yet.

## Build requirements

- SDK platform 37
- NDK `29.0.14206865`
- `cargo-ndk`
- Rust targets `aarch64-linux-android`, `armv7-linux-androideabi`, `i686-linux-android`, `x86_64-linux-android`

## Build

```shell
./gradlew :composeApp:assembleDebug
```

Debug builds default to `arm64-v8a`, while release builds default to all four supported Android ABIs.
Android-studio should detect the ABI of your target device automatically and adjust the call accordingly.

To control build ABIs manually:

```shell
./gradlew :composeApp:assembleDebug -Pandroid.injected.build.abi=x86_64
```
