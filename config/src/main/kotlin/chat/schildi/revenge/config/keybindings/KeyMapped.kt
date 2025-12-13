package chat.schildi.revenge.config.keybindings

import androidx.compose.ui.input.key.Key
import kotlinx.serialization.Serializable

/**
 * Key wrapper to make it serializable
 */
@Serializable
enum class KeyMapped(val key: Key) {
    // Alpha keys
    A(Key.A),
    B(Key.B),
    C(Key.C),
    D(Key.D),
    E(Key.E),
    F(Key.F),
    G(Key.G),
    H(Key.H),
    I(Key.I),
    J(Key.J),
    K(Key.K),
    L(Key.L),
    M(Key.M),
    N(Key.N),
    O(Key.O),
    P(Key.P),
    Q(Key.Q),
    R(Key.R),
    S(Key.S),
    T(Key.T),
    U(Key.U),
    V(Key.V),
    W(Key.W),
    X(Key.X),
    Y(Key.Y),
    Z(Key.Z),

    // Number keys
    Zero(Key.Zero),
    One(Key.One),
    Two(Key.Two),
    Three(Key.Three),
    Four(Key.Four),
    Five(Key.Five),
    Six(Key.Six),
    Seven(Key.Seven),
    Eight(Key.Eight),
    Nine(Key.Nine),

    // Arrow keys
    DirectionUp(Key.DirectionUp),
    DirectionDown(Key.DirectionDown),
    DirectionLeft(Key.DirectionLeft),
    DirectionRight(Key.DirectionRight),

    // Special keys
    Enter(Key.Enter),
    Escape(Key.Escape),
    MoveHome(Key.MoveHome),
    MoveEnd(Key.MoveEnd),
    Slash(Key.Slash),
    Semicolon(Key.Semicolon),

    // F-keys
    F1(Key.F1),
    F2(Key.F2),
    F3(Key.F3),
    F4(Key.F4),
    F5(Key.F5),
    F6(Key.F6),
    F7(Key.F7),
    F8(Key.F8),
    F9(Key.F9),
    F10(Key.F10),
    F11(Key.F11),
    F12(Key.F12),
}
