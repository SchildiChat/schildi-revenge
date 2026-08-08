# Android builds

## Build requirements

- Common dependencies mentioned in [README.md](README.md)
- Android SDK platform 37
- Android NDK
- `cargo-ndk`
- Rust targets `aarch64-linux-android`, `armv7-linux-androideabi`, `i686-linux-android`, `x86_64-linux-android`

## Build

```shell
./gradlew :androidApp:assembleDebug
```

Debug builds default to `arm64-v8a`, while release builds default to all four supported Android ABIs.
Android Studio should detect the ABI of your target device automatically and adjust the call accordingly.

To control build ABIs manually:

```shell
./gradlew :androidApp:assembleDebug -PandroidAbi=x86_64
```
