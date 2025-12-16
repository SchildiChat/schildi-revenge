package chat.schildi.revenge.config.keybindings

sealed interface Action {
    enum class Global : Action {
        Search,
        ToggleTheme,
        AutomaticTheme,
        ToggleHiddenItems,
        SetSetting,
        ToggleSetting,
    }
    enum class Navigation : Action {
        InboxInCurrent,
        AccountManagementInCurrent,
        InboxInNewWindow,
        AccountManagementInNewWindow,
        SplitHorizontal,
        SplitVertical,
    }
    enum class NavigationItem : Action {
        NavigateCurrent,
        NavigateInNewWindow,
    }
    enum class Focus : Action {
        FocusUp,
        FocusDown,
        FocusLeft,
        FocusRight,
        FocusTop,
        FocusCenter,
        FocusBottom,
        FocusParent,
    }
    enum class List : Action {
        ScrollToTop,
        ScrollToBottom,
    }
    enum class Split : Action {
        Close,
    }
    enum class Inbox : Action {
        SetSetting,
        ToggleSetting,
        SetAccountHidden,
        SetAccountSelected,
        SetAccountExclusivelySelected,
        ToggleAccountHidden,
        ToggleAccountSelected,
        ToggleAccountExclusivelySelected,
    }
    enum class Conversation : Action {
        SetSetting,
        ToggleSetting,
        FocusComposer,
        HideComposerIfEmpty,
        ComposeMessage,
        ComposeNotice,
        ComposeEmote,
        ComposerSend,
        ComposerInsertAddCursor,
    }
    enum class Event : Action {
        MarkRead,
        MarkReadPrivate,
        ComposeEdit,
        ComposeReply,
        ComposeReaction,
        CopyContent,
        CopyEventSource,
        CopyEventId,
        CopyMxId,
    }
}
