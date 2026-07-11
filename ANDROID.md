# [WIP] Experimental Android support

I'm playing around with getting Revenge to run on Android, because why not.
No, I'm not planning on replacing Next anytime soon.
No, you can't install it on Android yet.

## Building - Rust

To compile Android bindings, install Android SDK platform 37, NDK `29.0.14206865`, `cargo-ndk`, and the Rust targets
`aarch64-linux-android`, `armv7-linux-androideabi`, `i686-linux-android`, and `x86_64-linux-android`. Build the bindings
AAR with:

```shell
./gradlew :matrixRustBindings:assembleDebug
```

Debug builds default to `arm64-v8a`, while release builds default to all four supported Android ABIs. Android Studio's
injected ABI property overrides either default, for example:

```shell
./gradlew :matrixRustBindings:assembleDebug -Pandroid.injected.build.abi=x86_64
```

## TODOs to remember for later

- The Android `Application` must call `attachAppDirs()` before Matrix or configuration storage is initialized. This attaches
  the application context used by the multiplatform AppDirs implementation.
