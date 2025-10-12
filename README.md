# Schildi's Revenge

A desktop Matrix client that seeks revenge for all the pain suffered from maintaining an Element Web fork, built from scratch using the Rust SDK and Jetpack Compose Multiplatform.

## Goals

- Fully controllable via keyboard, ideally vim-inspired key-bindings where it makes sense
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

- [ ] Initial UI with login via password and recovery code
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

### Possible advanced features

- [ ] Verify other devices via emoji
- [ ] Threads??? (optional thread view, will want to show threaded messages in the main timeline by default as well)
