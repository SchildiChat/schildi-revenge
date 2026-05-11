package chat.schildi.preferences

import chat.schildi.revenge.AvailableLocales
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.model.ComposerFormat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.hint_composer_format_html
import shire.composeapp.generated.resources.hint_composer_format_markdown
import shire.composeapp.generated.resources.hint_composer_format_plain
import shire.composeapp.generated.resources.hint_settings
import shire.composeapp.generated.resources.pref_always_show_keyboard_focus
import shire.composeapp.generated.resources.pref_auto_hide_composer_summary
import shire.composeapp.generated.resources.pref_auto_hide_composer_title
import shire.composeapp.generated.resources.pref_category_conversation
import shire.composeapp.generated.resources.pref_category_conversation_summary
import shire.composeapp.generated.resources.pref_category_developer_options
import shire.composeapp.generated.resources.pref_category_dimensions
import shire.composeapp.generated.resources.pref_category_dimensions_summary
import shire.composeapp.generated.resources.pref_category_inbox
import shire.composeapp.generated.resources.pref_category_inbox_summary
import shire.composeapp.generated.resources.pref_category_keyboard_control
import shire.composeapp.generated.resources.pref_category_layout_weights
import shire.composeapp.generated.resources.pref_category_scale
import shire.composeapp.generated.resources.pref_category_spaces
import shire.composeapp.generated.resources.pref_category_spaces_summary
import shire.composeapp.generated.resources.pref_category_split_limits
import shire.composeapp.generated.resources.pref_category_split_screen
import shire.composeapp.generated.resources.pref_category_split_screen_summary
import shire.composeapp.generated.resources.pref_category_theme_dark
import shire.composeapp.generated.resources.pref_category_theme_light
import shire.composeapp.generated.resources.pref_category_typing_notices
import shire.composeapp.generated.resources.pref_close_to_tray_summary
import shire.composeapp.generated.resources.pref_close_to_tray_title
import shire.composeapp.generated.resources.pref_dark_theme_title
import shire.composeapp.generated.resources.pref_debug_avatar_render_states_title
import shire.composeapp.generated.resources.pref_disable_typing_notice_in_public_rooms_summary
import shire.composeapp.generated.resources.pref_disable_typing_notice_in_public_rooms_title
import shire.composeapp.generated.resources.pref_focus_follows_mouse_summary
import shire.composeapp.generated.resources.pref_focus_follows_mouse_title
import shire.composeapp.generated.resources.pref_font_scale
import shire.composeapp.generated.resources.pref_formatted_composer_preview_summary
import shire.composeapp.generated.resources.pref_formatted_composer_preview_title
import shire.composeapp.generated.resources.pref_framed_rop_spinner_summary
import shire.composeapp.generated.resources.pref_framed_rop_spinner_title
import shire.composeapp.generated.resources.pref_hide_empty_conversation_details_pane_summary
import shire.composeapp.generated.resources.pref_hide_empty_conversation_details_pane_title
import shire.composeapp.generated.resources.pref_hide_empty_inbox_pane_summary
import shire.composeapp.generated.resources.pref_hide_empty_inbox_pane_title
import shire.composeapp.generated.resources.pref_hide_message_authenticity_not_guaranteed_summary
import shire.composeapp.generated.resources.pref_hide_message_authenticity_not_guaranteed_title
import shire.composeapp.generated.resources.pref_hide_message_authenticity_warnings_in_bridged_chats_summary
import shire.composeapp.generated.resources.pref_hide_message_authenticity_warnings_in_bridged_chats_title
import shire.composeapp.generated.resources.pref_hide_window_decoration_summary
import shire.composeapp.generated.resources.pref_hide_window_decoration_title
import shire.composeapp.generated.resources.pref_inbox_conversation_dual_pane_min_width_title
import shire.composeapp.generated.resources.pref_conversation_details_split_min_width_title
import shire.composeapp.generated.resources.pref_conversation_details_split_summary
import shire.composeapp.generated.resources.pref_conversation_details_split_title
import shire.composeapp.generated.resources.pref_desktop_notifications_summary
import shire.composeapp.generated.resources.pref_desktop_notifications_title
import shire.composeapp.generated.resources.pref_initial_window_height_summary
import shire.composeapp.generated.resources.pref_initial_window_height_title
import shire.composeapp.generated.resources.pref_initial_window_width_summary
import shire.composeapp.generated.resources.pref_initial_window_width_title
import shire.composeapp.generated.resources.pref_layout_weight_conversation_title
import shire.composeapp.generated.resources.pref_layout_weight_inbox_title
import shire.composeapp.generated.resources.pref_layout_weight_room_details_title
import shire.composeapp.generated.resources.pref_layout_weight_settings_root_title
import shire.composeapp.generated.resources.pref_layout_weight_settings_title
import shire.composeapp.generated.resources.pref_max_width_conversation_title
import shire.composeapp.generated.resources.pref_max_width_inbox_title
import shire.composeapp.generated.resources.pref_max_width_room_details_title
import shire.composeapp.generated.resources.pref_max_width_settings_title
import shire.composeapp.generated.resources.pref_minimal_mode_summary
import shire.composeapp.generated.resources.pref_minimal_mode_title
import shire.composeapp.generated.resources.pref_prefer_dual_pane_inbox_summary
import shire.composeapp.generated.resources.pref_prefer_dual_pane_inbox_title
import shire.composeapp.generated.resources.pref_preferred_message_format_summary
import shire.composeapp.generated.resources.pref_preferred_message_format_title
import shire.composeapp.generated.resources.pref_render_scale
import shire.composeapp.generated.resources.pref_send_typing_notice_summary
import shire.composeapp.generated.resources.pref_send_typing_notice_title
import shire.composeapp.generated.resources.pref_show_dev_infos_summary
import shire.composeapp.generated.resources.pref_show_dev_infos_title
import shire.composeapp.generated.resources.pref_theme_follow_system_summary
import shire.composeapp.generated.resources.pref_theme_follow_system_title
import shire.composeapp.generated.resources.pref_view_hidden_events_title
import shire.composeapp.generated.resources.pref_view_redactions_title
import shire.composeapp.generated.resources.pref_window_transparency_summary
import shire.composeapp.generated.resources.pref_window_transparency_title
import shire.composeapp.generated.resources.pref_client_generated_unread_counts_summary
import shire.composeapp.generated.resources.pref_client_generated_unread_counts_title
import shire.composeapp.generated.resources.pref_compact_root_spaces_summary
import shire.composeapp.generated.resources.pref_compact_root_spaces_title
import shire.composeapp.generated.resources.pref_bury_low_priority_summary
import shire.composeapp.generated.resources.pref_bury_low_priority_title
import shire.composeapp.generated.resources.pref_category_chat_sorting
import shire.composeapp.generated.resources.pref_category_general
import shire.composeapp.generated.resources.pref_category_general_appearance
import shire.composeapp.generated.resources.pref_category_general_behaviour
import shire.composeapp.generated.resources.pref_category_general_summary
import shire.composeapp.generated.resources.pref_category_unread_counts
import shire.composeapp.generated.resources.pref_client_side_sort_by_unread_summary
import shire.composeapp.generated.resources.pref_client_side_sort_by_unread_title
import shire.composeapp.generated.resources.pref_dual_mention_unread_counts_summary
import shire.composeapp.generated.resources.pref_dual_mention_unread_counts_title
import shire.composeapp.generated.resources.pref_force_render_blurhash_summary
import shire.composeapp.generated.resources.pref_force_render_blurhash_title
import shire.composeapp.generated.resources.pref_indicate_unread_count_underestimates_summary
import shire.composeapp.generated.resources.pref_indicate_unread_count_underestimates_title
import shire.composeapp.generated.resources.pref_locale
import shire.composeapp.generated.resources.pref_locale_follow_system
import shire.composeapp.generated.resources.pref_pin_favorites_summary
import shire.composeapp.generated.resources.pref_pin_favorites_title
import shire.composeapp.generated.resources.pref_render_silent_unread_summary
import shire.composeapp.generated.resources.pref_render_silent_unread_title
import shire.composeapp.generated.resources.pref_sort_with_silent_unread_summary
import shire.composeapp.generated.resources.pref_sort_with_silent_unread_title
import shire.composeapp.generated.resources.pseudo_space_accounts_summary
import shire.composeapp.generated.resources.pseudo_space_accounts_title
import shire.composeapp.generated.resources.pseudo_space_dms
import shire.composeapp.generated.resources.pseudo_space_favorites
import shire.composeapp.generated.resources.pseudo_space_groups
import shire.composeapp.generated.resources.pseudo_space_hide_empty_unread
import shire.composeapp.generated.resources.pseudo_space_invites
import shire.composeapp.generated.resources.pseudo_space_notifications
import shire.composeapp.generated.resources.pseudo_space_spaceless
import shire.composeapp.generated.resources.pseudo_space_spaceless_groups
import shire.composeapp.generated.resources.pseudo_space_unread
import shire.composeapp.generated.resources.pref_pseudo_spaces_summary
import shire.composeapp.generated.resources.pref_pseudo_spaces_title
import shire.composeapp.generated.resources.pref_render_space_order_keys_title
import shire.composeapp.generated.resources.pref_space_all_rooms_summary
import shire.composeapp.generated.resources.pref_space_all_rooms_title
import shire.composeapp.generated.resources.pref_space_unread_counts_mode_chats
import shire.composeapp.generated.resources.pref_space_unread_counts_mode_hide
import shire.composeapp.generated.resources.pref_space_unread_counts_mode_messages
import shire.composeapp.generated.resources.pref_space_unread_counts_mode_title
import shire.composeapp.generated.resources.pref_threaded_replies_in_main_timeline_summary
import shire.composeapp.generated.resources.pref_threaded_replies_in_main_timeline_title
import shire.composeapp.generated.resources.pref_threads_as_details_summary
import shire.composeapp.generated.resources.pref_threads_as_details_title
import shire.composeapp.generated.resources.pref_url_previews_in_e2ee_rooms_summary
import shire.composeapp.generated.resources.pref_url_previews_in_e2ee_rooms_title
import shire.composeapp.generated.resources.pref_url_previews_require_explicit_links_summary
import shire.composeapp.generated.resources.pref_url_previews_require_explicit_links_title
import shire.composeapp.generated.resources.pref_url_previews_summary
import shire.composeapp.generated.resources.pref_url_previews_title
import java.util.Locale

object ScPrefs {

    val initialLocale: Locale = Locale.getDefault()
    const val LOCALE_DEFAULT = "en"
    val availableLocaleSettings = (
        listOf(ScListPrefEntry("", Res.string.pref_locale_follow_system.toStringHolder())) +
        (listOf(LOCALE_DEFAULT) + AvailableLocales.codes).map {
            ScListPrefEntry(it, Locale.forLanguageTag(it).displayName.toStringHolder())
        }
    ).toPersistentList()

    // Render scale
    val RENDER_SCALE = ScFloatPref("RENDER_SCALE", 1f, Res.string.pref_render_scale, minValue = 0.5f, maxValue = 5f, allowLiveSliderChange = false)
    val FONT_SCALE = ScFloatPref("FONT_SCALE", 1f, Res.string.pref_font_scale, minValue = 0.5f, maxValue = 5f, allowLiveSliderChange = false)

    // Layout limits
    val MAX_WIDTH_INBOX = ScIntPref("MAX_WIDTH_INBOX", 1024, Res.string.pref_max_width_inbox_title, minValue = 400, maxValue = 4000)
    val MAX_WIDTH_CONVERSATION = ScIntPref("MAX_WIDTH_CONVERSATION", 1600, Res.string.pref_max_width_conversation_title, minValue = 400, maxValue = 4000)
    val MAX_WIDTH_ROOM_DETAILS = ScIntPref("MAX_WIDTH_ROOM_DETAILS", 600, Res.string.pref_max_width_room_details_title, minValue = 200, maxValue = 4000)
    val MAX_WIDTH_SETTINGS = ScIntPref("MAX_WIDTH_SETTINGS", 1024, Res.string.pref_max_width_settings_title, minValue = 400, maxValue = 4000, allowLiveSliderChange = false)
    // Layout weights
    val LAYOUT_WEIGHT_INBOX = ScIntPref("LAYOUT_WEIGHT_INBOX", 80, Res.string.pref_layout_weight_inbox_title, minValue = 20, maxValue = 1000)
    val LAYOUT_WEIGHT_CONVERSATION = ScIntPref("LAYOUT_WEIGHT_CONVERSATION", 100, Res.string.pref_layout_weight_conversation_title, minValue = 20, maxValue = 1000)
    val LAYOUT_WEIGHT_ROOM_DETAILS = ScIntPref("LAYOUT_WEIGHT_ROOM_DETAILS", 40, Res.string.pref_layout_weight_room_details_title, minValue = 20, maxValue = 1000)
    val LAYOUT_WEIGHT_SETTINGS = ScIntPref("LAYOUT_WEIGHT_SETTINGS", 100, Res.string.pref_layout_weight_settings_title, minValue = 20, maxValue = 1000, allowLiveSliderChange = false)
    val LAYOUT_WEIGHT_SETTINGS_ROOT = ScIntPref("LAYOUT_WEIGHT_SETTINGS_ROOT", 80, Res.string.pref_layout_weight_settings_root_title, minValue = 20, maxValue = 1000, allowLiveSliderChange = false)

    // Notifications
    val DESKTOP_NOTIFICATIONS = ScBoolPref("DESKTOP_NOTIFICATIONS", true, Res.string.pref_desktop_notifications_title, Res.string.pref_desktop_notifications_summary)

    // Tray icon
    val CLOSE_TO_TRAY = ScBoolPref("CLOSE_TO_TRAY", true, Res.string.pref_close_to_tray_title, Res.string.pref_close_to_tray_summary)

    // Keyboard navigation
    val MINIMAL_MODE = ScBoolPref("MINIMAL_MODE", false, Res.string.pref_minimal_mode_title, Res.string.pref_minimal_mode_summary)
    val ALWAYS_SHOW_KEYBOARD_FOCUS = ScBoolPref("ALWAYS_SHOW_KEYBOARD_FOCUS", false, Res.string.pref_always_show_keyboard_focus)
    val FOCUS_FOLLOWS_MOUSE = ScBoolPref("FOCUS_FOLLOWS_MOUSE", false, Res.string.pref_focus_follows_mouse_title, Res.string.pref_focus_follows_mouse_summary)

    object SpaceUnreadCountMode {
        const val MESSAGES = "MESSAGES"
        const val CHATS = "CHATS"
        const val HIDE = "HIDE"
    }

    // Developer options
    val RENDER_AVATAR_STATES = ScBoolPref("RENDER_AVATAR_STATES", false, Res.string.pref_debug_avatar_render_states_title)
    val RENDER_SPACE_ORDER_KEYS = ScBoolPref("RENDER_SPACE_ORDER_KEYS", false, Res.string.pref_render_space_order_keys_title)
    val SHOW_DEV_INFOS = ScBoolPref("SHOW_DEV_INFOS", false, Res.string.pref_show_dev_infos_title, Res.string.pref_show_dev_infos_summary)
    val FORCE_RENDER_BLURHASH = ScBoolPref("FORCE_RENDER_BLURHASH", false, Res.string.pref_force_render_blurhash_title, Res.string.pref_force_render_blurhash_summary)
    val FRAME_DROP_SPINNER = ScBoolPref("FRAME_DROP_SPINNER", false, Res.string.pref_framed_rop_spinner_title, Res.string.pref_framed_rop_spinner_summary)

    // Appearance
    val LOCALE = ScStringListPref("LOCALE", "", availableLocaleSettings, Res.string.pref_locale, allowNonEntryValues = true)
    val INITIAL_WINDOW_WIDTH = ScIntPref("INITIAL_WINDOW_WIDTH", 900, Res.string.pref_initial_window_width_title, Res.string.pref_initial_window_width_summary, minValue = 600, maxValue = 3840)
    val INITIAL_WINDOW_HEIGHT = ScIntPref("INITIAL_WINDOW_HEIGHT", 1200, Res.string.pref_initial_window_height_title, Res.string.pref_initial_window_height_summary, minValue = 600, maxValue = 2160)
    val HIDE_WINDOW_DECORATION = ScBoolPref("HIDE_WINDOW_DECORATION", false, Res.string.pref_hide_window_decoration_title, Res.string.pref_hide_window_decoration_summary, requiresWindowRecreation = true)
    val BACKGROUND_ALPHA_LIGHT = ScFloatPref("BACKGROUND_ALPHA_LIGHT", 1f, Res.string.pref_window_transparency_title, Res.string.pref_window_transparency_summary, minValue = 0f, maxValue = 1f, dependencies = HIDE_WINDOW_DECORATION.asDependencies(), stepSize = 0.01f, stringFormat = "%.2f")
    val BACKGROUND_ALPHA_DARK = ScFloatPref("BACKGROUND_ALPHA_DARK", 1f, Res.string.pref_window_transparency_title, Res.string.pref_window_transparency_summary, minValue = 0f, maxValue = 1f, dependencies = HIDE_WINDOW_DECORATION.asDependencies(), stepSize = 0.01f, stringFormat = "%.2f")
    val THEME_FOLLOW_SYSTEM = ScBoolPref("THEME_FOLLOW_SYSTEM", true, Res.string.pref_theme_follow_system_title, Res.string.pref_theme_follow_system_summary)
    val THEME_DARK = ScBoolPref("THEME_DARK", false, Res.string.pref_dark_theme_title, dependencies = listOf(THEME_FOLLOW_SYSTEM.toDependency(expect = false)))

    val CLIENT_GENERATED_UNREAD_COUNTS = ScBoolPref("CLIENT_GENERATED_UNREAD_COUNTS", true, Res.string.pref_client_generated_unread_counts_title, Res.string.pref_client_generated_unread_counts_summary, disabledValue = true)
    val RENDER_SILENT_UNREAD = ScBoolPref("RENDER_SILENT_UNREAD", true, Res.string.pref_render_silent_unread_title, Res.string.pref_render_silent_unread_summary, disabledValue = false)
    val INDICATE_UNREAD_COUNT_UNDERESTIMATES = ScBoolPref("INDICATE_UNREAD_COUNT_UNDERESTIMATES", true, Res.string.pref_indicate_unread_count_underestimates_title, Res.string.pref_indicate_unread_count_underestimates_summary, disabledValue = false, dependencies = RENDER_SILENT_UNREAD.asDependencies())
    val PIN_FAVORITES = ScBoolPref("PIN_FAVORITES", false, Res.string.pref_pin_favorites_title, Res.string.pref_pin_favorites_summary)
    val BURY_LOW_PRIORITY = ScBoolPref("BURY_LOW_PRIORITY", false, Res.string.pref_bury_low_priority_title, Res.string.pref_bury_low_priority_summary)
    val SORT_BY_UNREAD = ScBoolPref("SORT_BY_UNREAD", false, Res.string.pref_client_side_sort_by_unread_title, Res.string.pref_client_side_sort_by_unread_summary)
    val SORT_WITH_SILENT_UNREAD = ScBoolPref(
        "SORT_WITH_SILENT_UNREAD",
        true,
        Res.string.pref_sort_with_silent_unread_title,
        Res.string.pref_sort_with_silent_unread_summary,
        disabledValue = false,
        dependencies = listOf(
            SORT_BY_UNREAD.toDependency(),
            RENDER_SILENT_UNREAD.toDependency(),
        )
    )
    val DUAL_MENTION_UNREAD_COUNTS = ScBoolPref("DUAL_MENTION_UNREAD_COUNTS", false, Res.string.pref_dual_mention_unread_counts_title, Res.string.pref_dual_mention_unread_counts_summary)
    val PREFER_DUAL_PANE_INBOX = ScBoolPref("PREFER_DUAL_PANE_INBOX", true, Res.string.pref_prefer_dual_pane_inbox_title, Res.string.pref_prefer_dual_pane_inbox_summary)
    val PREFER_CONVERSATION_DETAILS_SPLIT = ScBoolPref("PREFER_CONVERSATION_DETAILS_SPLIT", true, Res.string.pref_conversation_details_split_title, Res.string.pref_conversation_details_split_summary)
    val ALLOW_THREADS_IN_DETAILS_PANE = ScBoolPref("ALLOW_THREADS_IN_DETAILS_PANE", true, Res.string.pref_threads_as_details_title, Res.string.pref_threads_as_details_summary)
    val HIDE_EMPTY_INBOX_PANE = ScBoolPref("HIDE_EMPTY_INBOX_PANE", false, Res.string.pref_hide_empty_inbox_pane_title, Res.string.pref_hide_empty_inbox_pane_summary)
    val HIDE_EMPTY_CONVERSATION_DETAILS_PANE = ScBoolPref("HIDE_EMPTY_CONVERSATION_DETAILS_PANE", true, Res.string.pref_hide_empty_conversation_details_pane_title, Res.string.pref_hide_empty_conversation_details_pane_summary)
    val INBOX_CONVERSATION_SPLIT_MIN_WIDTH = ScIntPref("INBOX_CONVERSATION_SPLIT_MIN_WIDTH", 1000, Res.string.pref_inbox_conversation_dual_pane_min_width_title, minValue = 400, maxValue = 2000)
    val CONVERSATION_DETAILS_SPLIT_MIN_WIDTH = ScIntPref("CONVERSATION_DETAILS_SPLIT_MIN_WIDTH", 1000, Res.string.pref_conversation_details_split_min_width_title, minValue = 400, maxValue = 2000)

    // Spaces
    val COMPACT_ROOT_SPACES = ScBoolPref("COMPACT_ROOT_SPACES", false, Res.string.pref_compact_root_spaces_title, Res.string.pref_compact_root_spaces_summary)
    val SPACE_UNREAD_COUNTS = ScStringListPref(
        "SPACE_UNREAD_COUNTS",
        SpaceUnreadCountMode.MESSAGES,
        persistentListOf(
            ScListPrefEntry(SpaceUnreadCountMode.MESSAGES, Res.string.pref_space_unread_counts_mode_messages.toStringHolder()),
            ScListPrefEntry(SpaceUnreadCountMode.CHATS, Res.string.pref_space_unread_counts_mode_chats.toStringHolder()),
            ScListPrefEntry(SpaceUnreadCountMode.HIDE, Res.string.pref_space_unread_counts_mode_hide.toStringHolder()),
        ),
        Res.string.pref_space_unread_counts_mode_title,
        null,
    )
    val PSEUDO_SPACE_ALL_ROOMS = ScBoolPref("PSEUDO_SPACE_ALL_CHATS", true, Res.string.pref_space_all_rooms_title, Res.string.pref_space_all_rooms_summary)
    val PSEUDO_SPACE_FAVORITES = ScBoolPref("PSEUDO_SPACE_FAVORITES", true, Res.string.pseudo_space_favorites, null)
    val PSEUDO_SPACE_DMS = ScBoolPref("PSEUDO_SPACE_DMS", true, Res.string.pseudo_space_dms, null)
    val PSEUDO_SPACE_GROUPS = ScBoolPref("PSEUDO_SPACE_GROUPS", false, Res.string.pseudo_space_groups, null)
    val PSEUDO_SPACE_SPACELESS_GROUPS = ScBoolPref("PSEUDO_SPACE_SPACELESS_GROUPS", false, Res.string.pseudo_space_spaceless_groups, null)
    val PSEUDO_SPACE_SPACELESS = ScBoolPref("PSEUDO_SPACE_SPACELESS", false, Res.string.pseudo_space_spaceless, null)
    val PSEUDO_SPACE_NOTIFICATIONS = ScBoolPref("PSEUDO_SPACE_NOTIFICATIONS", true, Res.string.pseudo_space_notifications, null)
    val PSEUDO_SPACE_UNREAD = ScBoolPref("PSEUDO_SPACE_UNREAD", false, Res.string.pseudo_space_unread, null)
    val PSEUDO_SPACE_INVITES = ScBoolPref("PSEUDO_SPACE_INVITES", true, Res.string.pseudo_space_invites, null)
    val PSEUDO_SPACE_ACCOUNTS = ScBoolPref("PSEUDO_SPACE_ACCOUNTS", false, Res.string.pseudo_space_accounts_title, Res.string.pseudo_space_accounts_summary)
    val PSEUDO_SPACE_HIDE_EMPTY_UNREAD = ScBoolPref("PSEUDO_SPACE_HIDE_EMPTY_UNREAD", true, Res.string.pseudo_space_hide_empty_unread, null, dependencies = listOf(
        ScPrefFulfilledForAnyDependency(listOf(PSEUDO_SPACE_NOTIFICATIONS.toDependency(), PSEUDO_SPACE_UNREAD.toDependency(), PSEUDO_SPACE_INVITES.toDependency()))
    ))

    // Timeline
    val THREAD_REPLIES_IN_MAIN_TIMELINE = ScBoolPref("THREAD_REPLIES_IN_MAIN_TIMELINE", true, Res.string.pref_threaded_replies_in_main_timeline_title, Res.string.pref_threaded_replies_in_main_timeline_summary)
    val HIDE_MESSAGE_AUTHENTICITY_WARNINGS_IN_BRIDGED_CHATS = ScBoolPref("HIDE_MESSAGE_AUTHENTICITY_WARNINGS_IN_BRIDGED_CHATS", false, Res.string.pref_hide_message_authenticity_warnings_in_bridged_chats_title, Res.string.pref_hide_message_authenticity_warnings_in_bridged_chats_summary)
    val HIDE_AUTHENTICITY_NOT_GUARANTEED = ScBoolPref("HIDE_AUTHENTICITY_NOT_GUARANTEED", false, Res.string.pref_hide_message_authenticity_not_guaranteed_title, Res.string.pref_hide_message_authenticity_not_guaranteed_summary)
    val VIEW_HIDDEN_EVENTS = ScBoolPref("VIEW_HIDDEN_EVENTS", false, Res.string.pref_view_hidden_events_title)
    val VIEW_REDACTIONS = ScBoolPref("VIEW_REDACTIONS", false, Res.string.pref_view_redactions_title, dependencies = listOf(VIEW_HIDDEN_EVENTS.toDependency(expect = false)), disabledValue = true)
    val PREFERRED_MESSAGE_FORMAT = ScStringListPref(
        "PREFERRED_MESSAGE_FORMAT",
        defaultValue = ComposerFormat.MARKDOWN.toString(),
        items = persistentListOf(
            ScListPrefEntry(ComposerFormat.MARKDOWN.toString(), Res.string.hint_composer_format_markdown.toStringHolder()),
            ScListPrefEntry(ComposerFormat.PLAIN.toString(), Res.string.hint_composer_format_plain.toStringHolder()),
            ScListPrefEntry(ComposerFormat.HTML.toString(), Res.string.hint_composer_format_html.toStringHolder()),
        ),
        titleRes = Res.string.pref_preferred_message_format_title,
        summaryRes = Res.string.pref_preferred_message_format_summary,
    )
    val FORMATTED_COMPOSER_PREVIEW = ScBoolPref("FORMATTED_COMPOSER_PREVIEW", false, Res.string.pref_formatted_composer_preview_title, Res.string.pref_formatted_composer_preview_summary)
    val URL_PREVIEWS = ScBoolPref("URL_PREVIEWS", false, Res.string.pref_url_previews_title, Res.string.pref_url_previews_summary)
    val URL_PREVIEWS_IN_E2EE_ROOMS = ScBoolPref("URL_PREVIEWS_IN_E2EE_ROOMS", false, Res.string.pref_url_previews_in_e2ee_rooms_title, Res.string.pref_url_previews_in_e2ee_rooms_summary, dependencies = URL_PREVIEWS.asDependencies(), disabledValue = false)
    val URL_PREVIEWS_REQUIRE_EXPLICIT_LINKS = ScBoolPref("URL_PREVIEWS_REQUIRE_EXPLICIT_LINKS", true, Res.string.pref_url_previews_require_explicit_links_title, Res.string.pref_url_previews_require_explicit_links_summary, dependencies = URL_PREVIEWS.asDependencies(), disabledValue = null)

    // Composer
    val AUTO_HIDE_COMPOSER = ScBoolPref("AUTO_HIDE_COMPOSER", false, Res.string.pref_auto_hide_composer_title, Res.string.pref_auto_hide_composer_summary, dependencies = MINIMAL_MODE.asDependencies())
    val SEND_TYPING_NOTICE = ScBoolPref("SEND_TYPING_NOTICE", true, Res.string.pref_send_typing_notice_title, Res.string.pref_send_typing_notice_summary)
    val DISABLE_SEND_TYPING_NOTICE_IN_PUBLIC_ROOMS = ScBoolPref("DISABLE_SEND_TYPING_NOTICE_IN_PUBLIC_ROOMS", false, Res.string.pref_disable_typing_notice_in_public_rooms_title, Res.string.pref_disable_typing_notice_in_public_rooms_summary, dependencies = SEND_TYPING_NOTICE.asDependencies())

    val rootPrefs = ScPrefScreen("ROOT", Res.string.hint_settings, null, listOf<AbstractScPref>(
        ScPrefScreen("GENERAL", Res.string.pref_category_general, Res.string.pref_category_general_summary, listOf(
            LOCALE,
            CLOSE_TO_TRAY,
            DESKTOP_NOTIFICATIONS,
            ScPrefCategory("UI_SCALE", Res.string.pref_category_scale, null, listOf(
                RENDER_SCALE,
                FONT_SCALE,
            )),
            ScPrefCategory("GENERAL_APPEARANCE", Res.string.pref_category_general_appearance, null, listOf(
                THEME_FOLLOW_SYSTEM,
                THEME_DARK,
                HIDE_WINDOW_DECORATION,
                ScPrefCategory("THEME_LIGHT", Res.string.pref_category_theme_light, null, listOf(
                    BACKGROUND_ALPHA_LIGHT,
                )),
                ScPrefCategory("THEME_DARK", Res.string.pref_category_theme_dark, null, listOf(
                    BACKGROUND_ALPHA_DARK,
                )),
            )),
            ScPrefCategory("KEYBOARD_CONTROL", Res.string.pref_category_keyboard_control, null, listOf(
                MINIMAL_MODE,
                AUTO_HIDE_COMPOSER,
                FOCUS_FOLLOWS_MOUSE,
                ALWAYS_SHOW_KEYBOARD_FOCUS,
            )),
        )),
        ScPrefScreen("INBOX", Res.string.pref_category_inbox, Res.string.pref_category_inbox_summary, listOf(
            ScPrefCategory("INBOX_SORT", Res.string.pref_category_chat_sorting, null, listOf(
                SORT_BY_UNREAD,
                SORT_WITH_SILENT_UNREAD,
                PIN_FAVORITES,
                BURY_LOW_PRIORITY,
            )),
            ScPrefCategory("INBOX_UNREADS", Res.string.pref_category_unread_counts, null, listOf(
                RENDER_SILENT_UNREAD,
                INDICATE_UNREAD_COUNT_UNDERESTIMATES,
                DUAL_MENTION_UNREAD_COUNTS,
            )),
        )),
        ScPrefScreen("CONVERSATION", Res.string.pref_category_conversation, Res.string.pref_category_conversation_summary, listOf(
            THREAD_REPLIES_IN_MAIN_TIMELINE,
            VIEW_REDACTIONS,
            HIDE_AUTHENTICITY_NOT_GUARANTEED,
            HIDE_MESSAGE_AUTHENTICITY_WARNINGS_IN_BRIDGED_CHATS,
            PREFERRED_MESSAGE_FORMAT,
            FORMATTED_COMPOSER_PREVIEW,
            ScPrefCategory("TYPING_INDICATORS", Res.string.pref_category_typing_notices, null, listOf(
                SEND_TYPING_NOTICE,
                DISABLE_SEND_TYPING_NOTICE_IN_PUBLIC_ROOMS,
            )),
            ScPrefCategory("URL_PREVIEWS", Res.string.pref_url_previews_title, null, listOf(
                URL_PREVIEWS,
                URL_PREVIEWS_IN_E2EE_ROOMS,
                URL_PREVIEWS_REQUIRE_EXPLICIT_LINKS,
            )),
        )),
        ScPrefScreen("SPACES", Res.string.pref_category_spaces, Res.string.pref_category_spaces_summary, listOf(
            SPACE_UNREAD_COUNTS,
            COMPACT_ROOT_SPACES,
            ScPrefCategory("PSEUDO_SPACES", Res.string.pref_pseudo_spaces_title, Res.string.pref_pseudo_spaces_summary, listOf(
                PSEUDO_SPACE_ALL_ROOMS,
                PSEUDO_SPACE_FAVORITES,
                PSEUDO_SPACE_DMS,
                PSEUDO_SPACE_GROUPS,
                PSEUDO_SPACE_SPACELESS_GROUPS,
                PSEUDO_SPACE_SPACELESS,
                PSEUDO_SPACE_NOTIFICATIONS,
                PSEUDO_SPACE_UNREAD,
                PSEUDO_SPACE_INVITES,
                PSEUDO_SPACE_ACCOUNTS,
            )),
            ScPrefCategory("PSEUDO_SPACE_BEHAVIOR", Res.string.pref_category_general_behaviour, null, listOf(
                PSEUDO_SPACE_HIDE_EMPTY_UNREAD,
            )),
        )),
        ScPrefScreen("SPLIT", Res.string.pref_category_split_screen, Res.string.pref_category_split_screen_summary, listOf(
            PREFER_DUAL_PANE_INBOX,
            PREFER_CONVERSATION_DETAILS_SPLIT,
            ALLOW_THREADS_IN_DETAILS_PANE,
            HIDE_EMPTY_INBOX_PANE,
            HIDE_EMPTY_CONVERSATION_DETAILS_PANE,
            ScPrefCategory("SPLIT_LIMITS", Res.string.pref_category_split_limits, null, listOf(
                INBOX_CONVERSATION_SPLIT_MIN_WIDTH,
                CONVERSATION_DETAILS_SPLIT_MIN_WIDTH,
            )),
            ScPrefCategory("SPLIT_WEIGHTS", Res.string.pref_category_layout_weights, null, listOf(
                LAYOUT_WEIGHT_INBOX,
                LAYOUT_WEIGHT_CONVERSATION,
                LAYOUT_WEIGHT_ROOM_DETAILS,
                LAYOUT_WEIGHT_SETTINGS,
                LAYOUT_WEIGHT_SETTINGS_ROOT,
            )),
        )),
        ScPrefScreen("DIMENSIONS", Res.string.pref_category_dimensions, Res.string.pref_category_dimensions_summary, listOf(
            INITIAL_WINDOW_WIDTH,
            INITIAL_WINDOW_HEIGHT,
            MAX_WIDTH_INBOX,
            MAX_WIDTH_CONVERSATION,
            MAX_WIDTH_ROOM_DETAILS,
            MAX_WIDTH_SETTINGS,
        )),
        ScPrefScreen("DEVELOPER", Res.string.pref_category_developer_options, null, prefs = listOf(
            RENDER_AVATAR_STATES,
            VIEW_HIDDEN_EVENTS,
            RENDER_SPACE_ORDER_KEYS,
            SHOW_DEV_INFOS,
            FORCE_RENDER_BLURHASH,
            FRAME_DROP_SPINNER,
        )),
    ))

    val validSettingKeys = rootPrefs.prefs.collectScPrefs().map { it.sKey }.toSet()
    val validCategoryKeys = buildList {
        rootPrefs.forEachPreferenceOrContainer {
            (it as? ScPrefContainer)?.sKey?.let { key ->
                add(key)
            }
        }
    }.toSet()
}
