# Schildi's Revenge

A desktop Matrix client that seeks revenge for all the pain suffered from maintaining an Element Web fork, built from scratch using the Rust SDK and Jetpack Compose Multiplatform.

## Goals

- Fully controllable via keyboard, ideally vim-inspired key-bindings where it makes sense
- Prioritize yaml config (and account state?) over settings UI
- Multi-account without account switching
    - I.e. allows a merged inbox
    - But also allow filtering by account
- Hierarchical spaces
- Fast
- Multi-window
    - For individual chats
    - To have multiple inbox views open to allow viewing separate filters at once
- Design in the tradition of SchildiChat clients
- Easy to maintain, with as least effort as possible to make me happy
- Embrace command mode

## Non-goals

- Features I don't use or need
    - I may still accept PR's though
- Being a fully-featured Matrix client for users who rely on UI to get things done


## Implementation process

### MVP

- [ ] Get to compile with all necessary Rust SDK FFI bindings
    - [x] Figure out how to get working and complete bindings
        - `cargo build -p matrix-sdk-ffi --features rustls-tls`
        - `cargo run -p uniffi-bindgen -- generate --library --language kotlin --out-dir target/generated-bindings target/debug/libmatrix_sdk_ffi.so`
    - [x] Get hooked up in gradle
    - [ ] Gradle clean target
    - [ ] Gradle release flavor
    - [ ] Document known limitations in README
        - rebuild is slow and may be forgotten if Cargo.toml didn't update
        - Can always do `cargo clean` in `matrix-rust-sdk` to force a rebuild
- [x] Persistent storage
    - [x] MVP to get things working
    - [ ] Secure storage for passwords and keys and whatnot?
- [ ] Non-awful theme
- [ ] Initial config hooked in
    - [x] Accounts with username+homeserver (not: password and secrets)
    - [ ] Configurable key bindings
- [ ] Initial account management UI
    - [x] Login via password
    - [ ] Enter recovery code
    - [ ] Logout / delete account
- [ ] Lock to avoid running multiple instances in parallel, but instead bring to foreground?
- [ ] Inbox
    - [ ] List all chats
    - [ ] Make it look nice
    - [ ] Unread counts
    - [ ] Spaces navigation
    - [ ] Filter by spaces
    - [ ] Filter by account
    - [ ] Search
- [ ] Conversation view
    - [ ] View event source
    - [ ] Render text and notices
    - [ ] Send messages
    - [ ] Render read receipts
    - [ ] Render media
    - [ ] Render reactions
    - [ ] Render joins/leaves
    - [ ] Render link previews
    - [ ] Mechanism to select messages (for replis, edits, reactions...)
    - [ ] Send replies
    - [ ] Send reactions (just text input is enough for first run, OS may have emoji picker)
    - [ ] Send mentions
    - [ ] Delete messages
    - [ ] Mark as read, mark as unread (via keyboard shortcut)
    - [ ] Kick/ban users
    - [ ] Devtools
        - [ ] View room state
        - [ ] View account state
        - [ ] View room account state
        - [ ] Send room state
        - [ ] Send account state
        - [ ] Send custom events
- [ ] Room member list
- [ ] Commands
    - [ ] Create room
    - [ ] Start DM
    - [ ] Invite user
    - [ ] Set room notification settings
    - [ ] Copy room ID, room link
    - [ ] Set favorite / low priority
- [ ] Notifications
    - [ ] Allow muting individual accounts
- [ ] Tray icon with unread count
- [ ] Add release flavor that builds release variant of Rust SDK

### Possible advanced features

- [ ] Verify other devices via emoji
- [ ] MAS + rotating token support (need to re-persist session data)
- [ ] MAS: support generating QR for quick sign in of mobile devices
- [ ] Threads??? (optional thread view, will want to show threaded messages in the main timeline by default as well)
- [ ] Expose command line message interface to e.g. "open list for account X with filters Y"


## Building

This is a Kotlin Multiplatform project targeting Desktop (JVM).

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
      folder is the appropriate location.

### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
