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
- Long-term: Good enough for non-power-users to use mouse-only / intuitively

## Non-goals

- Features I don't use or need
    - I may still accept PR's though
- Being a fully-featured Matrix client for users who rely on UI to get things done


## Implementation process

### MVP

- [x] Schildi theme
- [ ] Initial config hooked in
    - [x] Accounts with username+homeserver (not: password and secrets)
    - [x] Configurable key bindings
- [x] Initial account management UI
    - [x] Login via password
    - [x] Enter recovery code
    - [x] Logout / delete account
- [ ] ScPrefs
    - [ ] Via toml
    - [ ] One datastore for user prefs
    - [ ] One datastore for app state (last selected space, opened destinations to restore, etc.)
    - [ ] UI?
- [ ] Lock to avoid running multiple instances in parallel, but instead bring to foreground?
- [ ] Inbox
    - [x] List all chats
    - [x] Make it look nice
    - [x] Unread counts
    - [ ] Spaces navigation
    - [ ] Filter by spaces
    - [ ] Filter by account
    - [x] Search
    - [ ] Handle invites
- [ ] Conversation view
    - [ ] View event source
    - [x] Render plain text and notices
    - [ ] Render formatted text and notices
    - [ ] Send text messages
    - [ ] Render read receipts
    - [ ] Render media
    - [x] Render reactions
    - [ ] Render joins/leaves
    - [ ] Render link previews
    - [ ] Render typing indicators
    - [ ] Mechanism to select messages (for replies, edits, reactions...)
    - [ ] Send replies
    - [ ] Send reactions (just text input is enough for first run, OS may have emoji picker)
    - [ ] Send mentions
    - [ ] Send attachments
    - [ ] Send typing indicators
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
- [ ] Account management screen polish
- [ ] Notifications
    - [ ] Allow muting individual accounts
- [ ] App icon
- [ ] Tray icon with unread count
    - [ ] Optional minimize to tray: last window close request doesn't discard window but just sets minimized
- [ ] Mouse-user-friendly mode
  - [ ] All screens accessible via mouse
  - [ ] All features accessible via mouse (e.g. search)
- [ ] Add release flavor that builds release variant of Rust SDK
- [ ] Release builds ready to install on various OSes

### Possible advanced features

- [ ] Verify other devices via emoji
- [ ] MAS + rotating token support (need to re-persist session data)
- [ ] MAS: support generating QR for quick sign in of mobile devices
- [ ] Threads??? (optional thread view, will want to show threaded messages in the main timeline by default as well)
- [ ] Expose command line message interface to e.g. "open list for account X with filters Y"
- [ ] Custom theming


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

### Building - Rust

SchildiChat uses the Matrix Rust SDK via FFI bindings. If you properly cloned the repository with the required
submodules, and you have a working Rust toolchain installed, the bindings should be generated automatically for you.
This additional SDK compiles will significantly slow down your first build time, but after that will only be needed once
the SDK changed. Usually the build process will pick up automatically whether to rebuild the SDK based on checking if
`Cargo.toml` changed. If you do some SDK changes, you may need to manually force a rebuild the SDK. To clean up previous
SDK compiles, run `cargo clean` in the `matrix-rust-sdk` directory. `./gradlew clean` *will not* clean a previous SDK
build (which is intended).


## Upstream SDK merges

For the Rust SDK itself, we can *almost* use the upstream SchildiChat Next repo, except that we need to disable the
`android_cleaner` feature for UniFFI generation.

Code in the `matrix` module is based on [Element X Android](https://github.com/element-hq/element-x-android).
This module makes interacting with the FFI bindings of the Matrix Rust SDK a lot nicer, e.g. by translating listeners
into kotlin flows, and freeing memory after copying data structures from the FFI-managed counterparts in order to avoid
memory leaks.

Whenever we update the Rust SDK, we thus also want to merge the related changes from Element X into our code. We cannot
use the upstream code as is, since it contains Android-specific code rather than being ready for Kotlin multiplatform.
Furthermore we do not want all of the Element X code, and we do not want to keep Element's very fine-grained module
structure.

So in order to do an upstream merge:
- Choose a working version of SchildiChat Next with the desired new changes
- Merge the appropriate SchildiChat Rust SDK revision into Revenge's `matrix-rust-sdk` git submodule
- Generate a cleaned version of SchildiChat Next:
    - `git clone https://github.com/SchildiChat/schildichat-android-next.git schildichat-next-revenge-skeleton`
    - `cd schildichat-next-to-revenge-skeleton`
    - `git filter-repo --paths-from-file /path/to/schildichat-revenge/elex_imports.txt`
- Merge the new cleaned version into a clone of the previous cleaned version
    - This is only necessary to support updating `elex_imports.txt`, as changing that will change cleaned commit history
    - If you need to update the upstream cleaning rules (e.g. add new modules), modify `bump_matrix_imports.sh` and
      then use it to re-generate `elex_imports.txt`.
- Merge the cleaned version of Next into Revenge using `git subtree`:
  cleaned Next repo:
    - `git remote add skeleton /path/to/schildichat-next-revenge-skeleton`
    - `git fetch skeleton`
    - `git subtree merge --prefix=matrix skeleton/main`


## Troubleshooting

### I'm using [insert Linux desktop/WM here], how to follow system dark mode?

When using some desktop environment or window manager that is not supported out of the box by
[Platform-Tools](https://github.com/kdroidFilter/Platform-Tools/blob/master/platformtools/darkmodedetector/src/jvmMain/kotlin/io/github/kdroidfilter/platformtools/darkmodedetector/linux/LinuxThemeDetector.kt),
but the way you toggle dark mode affects GTK applications, you can trick it by launching Revenge with an environment
variable of `XDG_CURRENT_DESKTOP=gnome`.

### I'm using wayland and HiDPI, everything looks blurry!

Until [compose multiplatform supports wayland natively](https://youtrack.jetbrains.com/issue/SKIKO-28),
the best solution I found is this hacky workaround:

- Install this [sommelier fork](https://github.com/akvadrako/sommelier)
- Launch Revenge via sommelier with the same scale setting that you use for HiDPI, e.g.
  `sommelier -X --scale=1.5 --data-driver=noop --shm-driver=noop --display=wayland-1 --socket=wayland-1 ./gradlew
  :compose:run`
