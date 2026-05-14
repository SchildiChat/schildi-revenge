/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 * Copyright 2026 SchildiChat
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package chat.schildi.revenge.model.verification

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.ic_verification_00
import shire.composeapp.generated.resources.ic_verification_01
import shire.composeapp.generated.resources.ic_verification_02
import shire.composeapp.generated.resources.ic_verification_03
import shire.composeapp.generated.resources.ic_verification_04
import shire.composeapp.generated.resources.ic_verification_05
import shire.composeapp.generated.resources.ic_verification_06
import shire.composeapp.generated.resources.ic_verification_07
import shire.composeapp.generated.resources.ic_verification_08
import shire.composeapp.generated.resources.ic_verification_09
import shire.composeapp.generated.resources.ic_verification_10
import shire.composeapp.generated.resources.ic_verification_11
import shire.composeapp.generated.resources.ic_verification_12
import shire.composeapp.generated.resources.ic_verification_13
import shire.composeapp.generated.resources.ic_verification_14
import shire.composeapp.generated.resources.ic_verification_15
import shire.composeapp.generated.resources.ic_verification_16
import shire.composeapp.generated.resources.ic_verification_17
import shire.composeapp.generated.resources.ic_verification_18
import shire.composeapp.generated.resources.ic_verification_19
import shire.composeapp.generated.resources.ic_verification_20
import shire.composeapp.generated.resources.ic_verification_21
import shire.composeapp.generated.resources.ic_verification_22
import shire.composeapp.generated.resources.ic_verification_23
import shire.composeapp.generated.resources.ic_verification_24
import shire.composeapp.generated.resources.ic_verification_25
import shire.composeapp.generated.resources.ic_verification_26
import shire.composeapp.generated.resources.ic_verification_27
import shire.composeapp.generated.resources.ic_verification_28
import shire.composeapp.generated.resources.ic_verification_29
import shire.composeapp.generated.resources.ic_verification_30
import shire.composeapp.generated.resources.ic_verification_31
import shire.composeapp.generated.resources.ic_verification_32
import shire.composeapp.generated.resources.ic_verification_33
import shire.composeapp.generated.resources.ic_verification_34
import shire.composeapp.generated.resources.ic_verification_35
import shire.composeapp.generated.resources.ic_verification_36
import shire.composeapp.generated.resources.ic_verification_37
import shire.composeapp.generated.resources.ic_verification_38
import shire.composeapp.generated.resources.ic_verification_39
import shire.composeapp.generated.resources.ic_verification_40
import shire.composeapp.generated.resources.ic_verification_41
import shire.composeapp.generated.resources.ic_verification_42
import shire.composeapp.generated.resources.ic_verification_43
import shire.composeapp.generated.resources.ic_verification_44
import shire.composeapp.generated.resources.ic_verification_45
import shire.composeapp.generated.resources.ic_verification_46
import shire.composeapp.generated.resources.ic_verification_47
import shire.composeapp.generated.resources.ic_verification_48
import shire.composeapp.generated.resources.ic_verification_49
import shire.composeapp.generated.resources.ic_verification_50
import shire.composeapp.generated.resources.ic_verification_51
import shire.composeapp.generated.resources.ic_verification_52
import shire.composeapp.generated.resources.ic_verification_53
import shire.composeapp.generated.resources.ic_verification_54
import shire.composeapp.generated.resources.ic_verification_55
import shire.composeapp.generated.resources.ic_verification_56
import shire.composeapp.generated.resources.ic_verification_57
import shire.composeapp.generated.resources.ic_verification_58
import shire.composeapp.generated.resources.ic_verification_59
import shire.composeapp.generated.resources.ic_verification_60
import shire.composeapp.generated.resources.ic_verification_61
import shire.composeapp.generated.resources.ic_verification_62
import shire.composeapp.generated.resources.ic_verification_63
import shire.composeapp.generated.resources.verification_emoji_00
import shire.composeapp.generated.resources.verification_emoji_01
import shire.composeapp.generated.resources.verification_emoji_02
import shire.composeapp.generated.resources.verification_emoji_03
import shire.composeapp.generated.resources.verification_emoji_04
import shire.composeapp.generated.resources.verification_emoji_05
import shire.composeapp.generated.resources.verification_emoji_06
import shire.composeapp.generated.resources.verification_emoji_07
import shire.composeapp.generated.resources.verification_emoji_08
import shire.composeapp.generated.resources.verification_emoji_09
import shire.composeapp.generated.resources.verification_emoji_10
import shire.composeapp.generated.resources.verification_emoji_11
import shire.composeapp.generated.resources.verification_emoji_12
import shire.composeapp.generated.resources.verification_emoji_13
import shire.composeapp.generated.resources.verification_emoji_14
import shire.composeapp.generated.resources.verification_emoji_15
import shire.composeapp.generated.resources.verification_emoji_16
import shire.composeapp.generated.resources.verification_emoji_17
import shire.composeapp.generated.resources.verification_emoji_18
import shire.composeapp.generated.resources.verification_emoji_19
import shire.composeapp.generated.resources.verification_emoji_20
import shire.composeapp.generated.resources.verification_emoji_21
import shire.composeapp.generated.resources.verification_emoji_22
import shire.composeapp.generated.resources.verification_emoji_23
import shire.composeapp.generated.resources.verification_emoji_24
import shire.composeapp.generated.resources.verification_emoji_25
import shire.composeapp.generated.resources.verification_emoji_26
import shire.composeapp.generated.resources.verification_emoji_27
import shire.composeapp.generated.resources.verification_emoji_28
import shire.composeapp.generated.resources.verification_emoji_29
import shire.composeapp.generated.resources.verification_emoji_30
import shire.composeapp.generated.resources.verification_emoji_31
import shire.composeapp.generated.resources.verification_emoji_32
import shire.composeapp.generated.resources.verification_emoji_33
import shire.composeapp.generated.resources.verification_emoji_34
import shire.composeapp.generated.resources.verification_emoji_35
import shire.composeapp.generated.resources.verification_emoji_36
import shire.composeapp.generated.resources.verification_emoji_37
import shire.composeapp.generated.resources.verification_emoji_38
import shire.composeapp.generated.resources.verification_emoji_39
import shire.composeapp.generated.resources.verification_emoji_40
import shire.composeapp.generated.resources.verification_emoji_41
import shire.composeapp.generated.resources.verification_emoji_42
import shire.composeapp.generated.resources.verification_emoji_43
import shire.composeapp.generated.resources.verification_emoji_44
import shire.composeapp.generated.resources.verification_emoji_45
import shire.composeapp.generated.resources.verification_emoji_46
import shire.composeapp.generated.resources.verification_emoji_47
import shire.composeapp.generated.resources.verification_emoji_48
import shire.composeapp.generated.resources.verification_emoji_49
import shire.composeapp.generated.resources.verification_emoji_50
import shire.composeapp.generated.resources.verification_emoji_51
import shire.composeapp.generated.resources.verification_emoji_52
import shire.composeapp.generated.resources.verification_emoji_53
import shire.composeapp.generated.resources.verification_emoji_54
import shire.composeapp.generated.resources.verification_emoji_55
import shire.composeapp.generated.resources.verification_emoji_56
import shire.composeapp.generated.resources.verification_emoji_57
import shire.composeapp.generated.resources.verification_emoji_58
import shire.composeapp.generated.resources.verification_emoji_59
import shire.composeapp.generated.resources.verification_emoji_60
import shire.composeapp.generated.resources.verification_emoji_61
import shire.composeapp.generated.resources.verification_emoji_62
import shire.composeapp.generated.resources.verification_emoji_63

internal data class EmojiResource(
    val drawableRes: DrawableResource,
    val nameRes: StringResource
)

internal fun Int.toEmojiResource(): EmojiResource {
    return when (this % 64) {
        0 -> EmojiResource(Res.drawable.ic_verification_00, Res.string.verification_emoji_00)
        1 -> EmojiResource(Res.drawable.ic_verification_01, Res.string.verification_emoji_01)
        2 -> EmojiResource(Res.drawable.ic_verification_02, Res.string.verification_emoji_02)
        3 -> EmojiResource(Res.drawable.ic_verification_03, Res.string.verification_emoji_03)
        4 -> EmojiResource(Res.drawable.ic_verification_04, Res.string.verification_emoji_04)
        5 -> EmojiResource(Res.drawable.ic_verification_05, Res.string.verification_emoji_05)
        6 -> EmojiResource(Res.drawable.ic_verification_06, Res.string.verification_emoji_06)
        7 -> EmojiResource(Res.drawable.ic_verification_07, Res.string.verification_emoji_07)
        8 -> EmojiResource(Res.drawable.ic_verification_08, Res.string.verification_emoji_08)
        9 -> EmojiResource(Res.drawable.ic_verification_09, Res.string.verification_emoji_09)
        10 -> EmojiResource(Res.drawable.ic_verification_10, Res.string.verification_emoji_10)
        11 -> EmojiResource(Res.drawable.ic_verification_11, Res.string.verification_emoji_11)
        12 -> EmojiResource(Res.drawable.ic_verification_12, Res.string.verification_emoji_12)
        13 -> EmojiResource(Res.drawable.ic_verification_13, Res.string.verification_emoji_13)
        14 -> EmojiResource(Res.drawable.ic_verification_14, Res.string.verification_emoji_14)
        15 -> EmojiResource(Res.drawable.ic_verification_15, Res.string.verification_emoji_15)
        16 -> EmojiResource(Res.drawable.ic_verification_16, Res.string.verification_emoji_16)
        17 -> EmojiResource(Res.drawable.ic_verification_17, Res.string.verification_emoji_17)
        18 -> EmojiResource(Res.drawable.ic_verification_18, Res.string.verification_emoji_18)
        19 -> EmojiResource(Res.drawable.ic_verification_19, Res.string.verification_emoji_19)
        20 -> EmojiResource(Res.drawable.ic_verification_20, Res.string.verification_emoji_20)
        21 -> EmojiResource(Res.drawable.ic_verification_21, Res.string.verification_emoji_21)
        22 -> EmojiResource(Res.drawable.ic_verification_22, Res.string.verification_emoji_22)
        23 -> EmojiResource(Res.drawable.ic_verification_23, Res.string.verification_emoji_23)
        24 -> EmojiResource(Res.drawable.ic_verification_24, Res.string.verification_emoji_24)
        25 -> EmojiResource(Res.drawable.ic_verification_25, Res.string.verification_emoji_25)
        26 -> EmojiResource(Res.drawable.ic_verification_26, Res.string.verification_emoji_26)
        27 -> EmojiResource(Res.drawable.ic_verification_27, Res.string.verification_emoji_27)
        28 -> EmojiResource(Res.drawable.ic_verification_28, Res.string.verification_emoji_28)
        29 -> EmojiResource(Res.drawable.ic_verification_29, Res.string.verification_emoji_29)
        30 -> EmojiResource(Res.drawable.ic_verification_30, Res.string.verification_emoji_30)
        31 -> EmojiResource(Res.drawable.ic_verification_31, Res.string.verification_emoji_31)
        32 -> EmojiResource(Res.drawable.ic_verification_32, Res.string.verification_emoji_32)
        33 -> EmojiResource(Res.drawable.ic_verification_33, Res.string.verification_emoji_33)
        34 -> EmojiResource(Res.drawable.ic_verification_34, Res.string.verification_emoji_34)
        35 -> EmojiResource(Res.drawable.ic_verification_35, Res.string.verification_emoji_35)
        36 -> EmojiResource(Res.drawable.ic_verification_36, Res.string.verification_emoji_36)
        37 -> EmojiResource(Res.drawable.ic_verification_37, Res.string.verification_emoji_37)
        38 -> EmojiResource(Res.drawable.ic_verification_38, Res.string.verification_emoji_38)
        39 -> EmojiResource(Res.drawable.ic_verification_39, Res.string.verification_emoji_39)
        40 -> EmojiResource(Res.drawable.ic_verification_40, Res.string.verification_emoji_40)
        41 -> EmojiResource(Res.drawable.ic_verification_41, Res.string.verification_emoji_41)
        42 -> EmojiResource(Res.drawable.ic_verification_42, Res.string.verification_emoji_42)
        43 -> EmojiResource(Res.drawable.ic_verification_43, Res.string.verification_emoji_43)
        44 -> EmojiResource(Res.drawable.ic_verification_44, Res.string.verification_emoji_44)
        45 -> EmojiResource(Res.drawable.ic_verification_45, Res.string.verification_emoji_45)
        46 -> EmojiResource(Res.drawable.ic_verification_46, Res.string.verification_emoji_46)
        47 -> EmojiResource(Res.drawable.ic_verification_47, Res.string.verification_emoji_47)
        48 -> EmojiResource(Res.drawable.ic_verification_48, Res.string.verification_emoji_48)
        49 -> EmojiResource(Res.drawable.ic_verification_49, Res.string.verification_emoji_49)
        50 -> EmojiResource(Res.drawable.ic_verification_50, Res.string.verification_emoji_50)
        51 -> EmojiResource(Res.drawable.ic_verification_51, Res.string.verification_emoji_51)
        52 -> EmojiResource(Res.drawable.ic_verification_52, Res.string.verification_emoji_52)
        53 -> EmojiResource(Res.drawable.ic_verification_53, Res.string.verification_emoji_53)
        54 -> EmojiResource(Res.drawable.ic_verification_54, Res.string.verification_emoji_54)
        55 -> EmojiResource(Res.drawable.ic_verification_55, Res.string.verification_emoji_55)
        56 -> EmojiResource(Res.drawable.ic_verification_56, Res.string.verification_emoji_56)
        57 -> EmojiResource(Res.drawable.ic_verification_57, Res.string.verification_emoji_57)
        58 -> EmojiResource(Res.drawable.ic_verification_58, Res.string.verification_emoji_58)
        59 -> EmojiResource(Res.drawable.ic_verification_59, Res.string.verification_emoji_59)
        60 -> EmojiResource(Res.drawable.ic_verification_60, Res.string.verification_emoji_60)
        61 -> EmojiResource(Res.drawable.ic_verification_61, Res.string.verification_emoji_61)
        62 -> EmojiResource(Res.drawable.ic_verification_62, Res.string.verification_emoji_62)
        63 -> EmojiResource(Res.drawable.ic_verification_63, Res.string.verification_emoji_63)
        else -> error("Cannot happen ($this)!")
    }
}
