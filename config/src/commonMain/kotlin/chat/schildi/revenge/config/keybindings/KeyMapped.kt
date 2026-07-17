package chat.schildi.revenge.config.keybindings

import androidx.compose.ui.input.key.Key
import kotlinx.serialization.Serializable

/**
 * Key wrapper to make it serializable
 */
@Serializable
enum class KeyMapped(val key: Key, val displayName: String) {
    // Alpha keys
    A(Key.A, "A"),
    B(Key.B, "B"),
    C(Key.C, "C"),
    D(Key.D, "D"),
    E(Key.E, "E"),
    F(Key.F, "F"),
    G(Key.G, "G"),
    H(Key.H, "H"),
    I(Key.I, "I"),
    J(Key.J, "J"),
    K(Key.K, "K"),
    L(Key.L, "L"),
    M(Key.M, "M"),
    N(Key.N, "N"),
    O(Key.O, "O"),
    P(Key.P, "P"),
    Q(Key.Q, "Q"),
    R(Key.R, "R"),
    S(Key.S, "S"),
    T(Key.T, "T"),
    U(Key.U, "U"),
    V(Key.V, "V"),
    W(Key.W, "W"),
    X(Key.X, "X"),
    Y(Key.Y, "Y"),
    Z(Key.Z, "Z"),

    // Number keys
    Zero(Key.Zero, "0"),
    One(Key.One, "1"),
    Two(Key.Two, "2"),
    Three(Key.Three, "3"),
    Four(Key.Four, "4"),
    Five(Key.Five, "5"),
    Six(Key.Six, "6"),
    Seven(Key.Seven, "7"),
    Eight(Key.Eight, "8"),
    Nine(Key.Nine, "9"),

    // Arrow keys
    DirectionUp(Key.DirectionUp, "Up"),
    DirectionDown(Key.DirectionDown, "Down"),
    DirectionLeft(Key.DirectionLeft, "Left"),
    DirectionRight(Key.DirectionRight, "Right"),

    // Special keys
    Enter(Key.Enter, "Enter"),
    Escape(Key.Escape, "Escape"),
    MoveHome(Key.MoveHome, "Home"),
    MoveEnd(Key.MoveEnd, "End"),
    Delete(Key.Delete, "Delete"),
    Backspace(Key.Backspace, "Backspace"),
    Slash(Key.Slash, "/"),
    Semicolon(Key.Semicolon, ";"),
    Plus(Key.Plus, "+"),
    Comma(Key.Comma, ","),
    Period(Key.Period, "."),
    Equals(Key.Equals, "="),
    Tab(Key.Tab, "Tab"),
    Minus(Key.Minus, "-"),

    // F-keys
    F1(Key.F1, "F1"),
    F2(Key.F2, "F2"),
    F3(Key.F3, "F3"),
    F4(Key.F4, "F4"),
    F5(Key.F5, "F5"),
    F6(Key.F6, "F6"),
    F7(Key.F7, "F7"),
    F8(Key.F8, "F8"),
    F9(Key.F9, "F9"),
    F10(Key.F10, "F10"),
    F11(Key.F11, "F11"),
    F12(Key.F12, "F12"),
}

fun Key.displayName(): String = KeyMapped.entries
    .find { it.key.keyCode == keyCode }
    ?.displayName
    ?: keyCode.toString()
