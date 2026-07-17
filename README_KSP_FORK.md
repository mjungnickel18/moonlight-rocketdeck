# Moonlight KSP — Portrait Keyboard Fork

A fork of [moonlight-android](https://github.com/moonlight-stream/moonlight-android) for playing
**Kerbal Space Program** via game streaming in **portrait mode**: the video is docked at the top
of the screen (full width, letterboxed to the stream's aspect ratio) and the space below it is
filled with a KSP-specific on-screen keyboard.

## What changed vs. upstream

- **`com.limelight.ui.KspKeyboardView`** (new): a custom-drawn on-screen keyboard.
  Keys behave like a physical keyboard — key-down on touch, key-up on release — with full
  multi-touch, so chords like *Shift+W* (throttle up while pitching) work. Presses give
  haptic feedback.
- **`Game.java`**: when the *KSP portrait keyboard* setting is enabled (default on), the
  stream activity requests portrait orientation, pins the `StreamView` to the top of the
  screen, and docks the keyboard into the remaining space below it. Key events are injected
  through Moonlight's normal keyboard path (`keyboardEvent` → `KeyboardTranslator` →
  `sendKeyboardInput`), so modifier tracking and server-side key state work exactly like a
  real keyboard.
- **Settings**: *Input Settings → KSP portrait keyboard* toggles the whole feature. Turning
  it off restores stock Moonlight behavior (landscape, no keyboard).
- **App ID / label**: changed to `com.limelight.kspkeyboard` / "Moonlight KSP" per upstream's
  request that forks not reuse the official application ID.

## Keyboard layout (stock KSP bindings)

| Row | Keys |
|-----|------|
| 1 | Esc (Pause), F5 (Quicksave), F9 (Quickload), M (Map), V (Camera), C (IVA), ⌨ (toggle Android IME for text entry), **ABORT** (Backspace) |
| 2 | 1–0 (action groups) |
| 3 | T (SAS), R (RCS), G (Gear), B (Brakes), U (Lights), F (SAS hold), ◀/▶ (time warp −/+), ×1 (cancel warp) |
| 4 | Q W E (roll/pitch), I H N (RCS translate), **THR ▲** (Shift, hold) |
| 5 | A S D (yaw/pitch), J K L (RCS translate), **THR ▼** (Ctrl, hold) |
| 6 | Z (full throttle), X (cut throttle), **STAGE** (Space), FINE (CapsLock precision), \[ \] (switch vessel) |

The ⌨ key toggles the local Android soft keyboard for free-form typing (vessel names,
save names, etc.).

## Building

Requires JDK 17+, Android SDK 34, NDK 27.0.12077973.

```
gradlew assembleNonRootDebug
```

APK output: `app/build/outputs/apk/nonRoot/debug/`

## License

GPL-3.0, same as upstream moonlight-android.
