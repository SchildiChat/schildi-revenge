[![Matrix room #revenge:schildi.chat](https://img.shields.io/matrix/revenge:schildi.chat.svg?label=%23revenge:schildi.chat&logo=matrix&server_fqdn=matrix.org)](https://matrix.to/#/#revenge:schildi.chat)

# SchildiChat Revenge

A desktop Matrix client written in kotlin / Compose Multiplatform based on the Matrix Rust SDK.

## Main goals

- Fully controllable via keyboard, including command mode and highly configurable key-bindings
- Native multi-account from the start, not as an afterthought
    - Allows a merged inbox
    - But also allow filtering by account if desired
- Hierarchical spaces
    - Including mutli-account space merging if it's the same space with a shared parent spaces
- Faster than the old web-based client
- Multi-window
    - For individual chats
    - To have multiple inbox views open to allow viewing separate filters at once
- Design in the tradition of SchildiChat clients
- Long-term: Good enough for non-power-users to use mouse-only / intuitively


## Initial implementation checklist

This is a rough collection of features that I consider important for core use, to help with the initial bringup.
This list is not sufficient to consider the client feature-complete in any way.

- [x] Schildi theme
- [x] Initial config hooked in
    - [x] Accounts with username+homeserver (not: password and secrets)
    - [x] Configurable key bindings
- [x] Initial account management UI
    - [x] Login via password
    - [x] Enter recovery code
    - [x] Logout / delete account
- [x] ScPrefs
    - [x] Via toml
    - [x] Datastore for user prefs
    - [x] Datastore for other app state
    - [x] UI
- [x] Lock to avoid running multiple instances in parallel, but instead bring to foreground?
- [ ] Inbox
    - [x] List all chats
    - [x] Make it look nice
    - [x] Unread counts
    - [x] Spaces navigation
    - [x] Filter by spaces
    - [x] Filter by account
    - [x] Search
    - [ ] Handle invites
        - [x] Join
        - [ ] Reject
        - [ ] Reject & Ignore
- [ ] Conversation view
    - [x] Render plain text and notices
    - [x] Render formatted text and notices
    - [x] Send text messages
    - [x] Render read receipts
    - [ ] Render media
        - [x] Images
        - [ ] Videos
        - [ ] Audio
        - [x] Files
    - [x] Render reactions
    - [x] Render joins/leaves
    - [x] Render link previews
    - [x] Render typing indicators
    - [x] Mechanism to select messages (for replies, edits, reactions...)
    - [x] Send replies
    - [x] Send reactions (just text input is enough for first run, can use `:` shortcodes)
    - [x] Send mentions
    - [x] Send attachments
        - [x] Via file picker
        - [x] Via drag&drop ([broken from some wayland
          windows](https://gitlab.freedesktop.org/wlroots/wlroots/-/merge_requests/841), sorry)
        - [x] Via copy&paste (if configured in key bindings)
    - [ ] Send typing indicators
    - [x] Delete messages
    - [x] Mark as read (via command)
    - [x] Mark as unread (via command)
    - [x] Kick/ban users (via command)
    - [ ] Devtools
        - [x] Copy room state to clipboard
        - [ ] View account state
        - [ ] View room account state
        - [x] Send custom events
        - [x] Send room state
        - [ ] Send account state
        - [ ] Send room account state
    - [x] User trust MVP
        - [x] Message authenticity shields
        - [x] Identity resets
    - [ ] Failed message send retries: `EventTimelineHandler.sendHandleProvider()`
- [ ] Room member list
- [ ] Commands
    - [ ] Create room
    - [ ] Start DM
    - [x] Invite user
    - [x] Kick/ban user
    - [ ] Set room notification settings
    - [x] Copy room ID, room link
    - [x] Set favorite / low priority
- [ ] Account management screen polish
- [ ] Notifications
    - [x] Allow muting individual accounts
- [ ] App icon
- [ ] Tray icon with unread count
    - [x] Optional minimize to tray: last window close request doesn't discard window but just sets minimized
- [x] Mouse-user-friendly mode
  - [x] All screens accessible via mouse
  - [x] Important features accessible via mouse (e.g. search)
- [ ] Add release flavor that builds release variant of Rust SDK
- [ ] Release builds ready to install on various OSes

### More features for "later"

- [ ] Mark as read automatically while you scroll
- [ ] Verify other devices via emoji
- [ ] MAS + rotating token support (need to re-persist session data)
- [ ] MAS: support generating QR for quick sign in of mobile devices
- [ ] Threads (optional thread view, will want to show threaded messages in the main timeline by default as well)
- [ ] Expose command line message interface to e.g. "open list for account X with filters Y"
- [ ] More UI for "advanced" features
    - [ ] Account settings
        - [ ] Own user profile settings
        - [ ] Change user password
        - [ ] Reset user identity
        - [ ] Session management to rename or log out other devices
    - [ ] Room settings
    - [ ] Create new rooms
    - [ ] ...
- [ ] Custom theming


## Customizable keybindings

Schildi Revenge has highly customizable keyboard actions (that I still need to properly document at some point).  
To customize them, copy either
[`keybindings-default.toml`](https://github.com/SchildiChat/schildi-revenge/blob/main/composeApp/src/jvmMain/composeResources/files/keybindings-default.toml)
(the default config if not customized) or
[the more opinionated example config](https://github.com/SchildiChat/schildi-revenge/blob/main/example-config/keybindings.toml)
to `~/.config/SchildiChatRevenge/keybindings.toml` (on typical Linux systems)
or `AppData/Local/SchildiChatRevenge/keybindings.toml` (on Windows).

Once the config exists, SchildiChat will reload its settings automatically whenever you edit the file even while
SchildiChat is running, to make it easier trying out different settings without having to restart.

The config is structured in multiple sections that are applicable in different contexts in the app.
The sections include:

- `global`: Applicable everywhere.
- `appMessage`: Actions related to app prompts at the bottom of the screen, like error messages or confirmation prompts.
- `inbox`: Your room list.
- `room`: Anything related to a room, e.g. while in the conversation screen viewing the message timeline, but also
  applicable in inbox when focusing a certain roonm.
- `conversation`: While viewing a room's message timeline.
- `event`: Actions specific to messages and other timeline content, when focused in the conversation screen.
- `navigation`: Similar to `global` actions but specific to navigating the app.
- `navigationItem`: Related to UI elements which can navigate you to other parts of the app on interaction, such as room
  list entries in your inbox.
- `focus`: Similar to `global` but specific to moving the keyboard-based focus to other items.
- `list`: Applicable if your current focus is within a list of some kind (like a room list, or a list of messages in
  your timeline).
- `split`: Actions related to an experimental split screen mode.

Until I properly document all available actions, you can find them defined
[here](https://github.com/SchildiChat/schildi-revenge/blob/main/config/src/main/kotlin/chat/schildi/revenge/config/keybindings/Action.kt).
Note that as long as you have a keybinding for `Command` in the `global` section, you can also type in any of these
action into the command mode to play around with them before defining key bindings for them.

Each key binding needs to hold at least a `key` and an `action` value.
Additional arguments may be optional or mandatory depending on the selected action, and include:
- `shift`, `ctrl`, `alt`: set to `true` if you want to require certain modifier keys for your keybinding.
- `args`: An array of arguments to pass to the `action`.
- `destinations`: An array of destinations that you want to restrict this key binding to. Useful e.g. if you want to be
  able to toggle certain settings but only on a specific screen. Possible values include e.g. `Inbox` and
  `Conversation`. A complete list of destination names can be found
  [here](https://github.com/SchildiChat/schildi-revenge/blob/main/composeApp/src/jvmMain/kotlin/chat/schildi/revenge/Destination.kt).


## Building

### Build dependencies

- JDK 21
- Rust toolchain / `cargo`

Windows-specific hints for my own notes:

- the normal Oracle JDK seems to be automatically picked up by git bash
- Rust prompts to install some Visual Studio dependencies - selecting the "Quick install" seems to work fine
- Need to also install C++ tools via Visual Studio installer for Rust dependencies to work
    - If it didn't install automatically the first time, can start the installer again from the Downloads directory
    - Select "Desktop Development with C++" from the workloads tab

### Build and run in one go

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

### Building - Rust

SchildiChat uses the Matrix Rust SDK via FFI bindings. If you properly cloned the repository with the required
submodules, and you have a working Rust toolchain installed, the bindings should be generated automatically for you.
This additional SDK compiles will significantly slow down your first build time, but after that will only be needed once
the SDK changed. Usually the build process will pick up automatically whether to rebuild the SDK based on checking if
`Cargo.toml` changed. If you do some SDK changes, you may need to manually force a rebuild the SDK. To clean up previous
SDK compiles, run `cargo clean` in the `matrix-rust-sdk` directory. `./gradlew clean` *will not* clean a previous SDK
build (which is intended).

## Building release builds

[For now, no cross-compilation for Windows targets from Linux](https://youtrack.jetbrains.com/issue/CMP-2918).

For Linux releases:

```
./package_appimage.sh
```

For Windows releases:

```
./gradlew packageMsi
```


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
the best solution *depends* (I have not found a fully satisfying solution yet).

#### Disable scaling for xwayland

If your window manager / desktop environment supports disabling scaling for xwayland applications, that's probably the
best. You can then scale Revenge via the `RENDER_SCALE` setting. If you read this before I built a preferences UI for
it, put `RENDER_SCALE=2.0` (or the scale of your choice) in `$HOME/.config/SchildiChatRevenge/preferences.toml`.

- [hyprland support this out of the box](https://deepwiki.com/hyprwm/hyprland-wiki/4.4-xwayland-integration) via `force_zero_scaling`.

#### Use xwayland-xprop + wlroots-hidpi-xprop

If you can configure your WM to use a custom xwayland implementation, you may have luck with
[wlroots-hidpi-xprop](https://aur.archlinux.org/packages/wlroots-hidpi-xprop-git). E.g. for sway on Arch Linux:

- Install `xorg-xwayland-hidpi-xprop`, `wlroots-hidpi-xprop-git`, `sway-git`
- Run `xprop -root -format _XWAYLAND_GLOBAL_OUTPUT_SCALE 32c -set _XWAYLAND_GLOBAL_OUTPUT_SCALE 2`

Here you ideally should enter the correct scale that also your wayland desktop environment uses (or maybe it even picks
it automatically?).
However it only supports full integer scaling. If you're using fractional scaling, you may have luck by choosing a
higher scale for xwayland (which will make xwayland apps even smaller), but then compensating that by choosing a higher
application-side scaling - for SchildiChat Revenge via the `RENDER_SCALE` setting.
If you read this before I built a preferences UI for it, put `RENDER_SCALE=2.0` (or the scale of your choice) in `$HOME/.config/SchildiChatRevenge/preferences.toml`.

#### sommelier

- Install this [sommelier fork](https://github.com/akvadrako/sommelier). You probably need to compile it yourself.
- Launch Revenge via sommelier with the same scale setting that you use for HiDPI, e.g.
  `sommelier -X --scale=1.5 --data-driver=noop --shm-driver=noop --display=wayland-1 --socket=wayland-1 ./gradlew
  :compose:run`
- I found it to have some issues with mouse input towards the edges that I haven't figured out how to resolve yet,
  so if you prefer to use your mouse over your keyboard this may not be a good solution for you.

#### Patch SchildiChat Revenge to get Compose to run without AWT (?)

It seems to be somewhat possible to use a different rendering backend for jetpack compose, [like
LWJGL](https://github.com/JetBrains/compose-multiplatform/tree/master/experimental/lwjgl-integration)
[another link](https://github.com/JetBrains/compose-multiplatform/issues/652),
which could in theory support wayland natively? But may introduce other bugs? Outcome unclear.
