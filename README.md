# Moonlight KSP — Kerbal Space Program Streaming Client

A fork of [Moonlight for Android](https://github.com/moonlight-stream/moonlight-android)
purpose-built for playing **Kerbal Space Program** via game streaming
([Sunshine](https://github.com/LizardByte/Sunshine) or NVIDIA GameStream) on a phone
or tablet — with a KSP-specific on-screen keyboard laid out **around** the video
instead of on top of it, so your fingers never cover the action.

## Layouts

**Portrait** (phones): video docked at the top at the stream's aspect ratio, a
7-row keyboard filling the rest of the screen.

**Landscape**: video top-center, flight controls (QWE/ASD, throttle) on the left,
toggles/staging on the right, action groups in a strip along the bottom. The
layout adapts to the screen: on wide screens (19.5:9 phones) the video keeps
nearly full height; on 16:10 tablets the side panels get a guaranteed minimum
width and the bottom strip grows a second row with the RCS docking cluster.
Rotating the device switches layouts live without restarting the stream.

## Keys

Keys behave like a physical keyboard — key-down on touch, key-up on release,
full multi-touch — so held keys and chords (Shift+W: throttle up while pitching)
work exactly as at a desk. All stock KSP bindings are covered:

- **Flight**: Q/W/E, A/S/D, Shift/Ctrl (hold to throttle), Z/X (full/cut), Space (stage)
- **Toggles**: T (SAS), R (RCS), G (gear), B (brakes), U (lights), F, CapsLock (precision)
- **Systems**: action groups 1–0, Backspace (abort), M (map), V/C (camera), time warp `,` `.` `/`
- **Docking**: H/N/J/L/I/K RCS translation
- **Mouse**: LMB/RMB as hold-buttons (dragging works), auto-repeating scroll wheel keys
- **⌨** toggles the Android keyboard for free text entry (vessel names, save games)

The whole feature can be disabled under *Settings → Input Settings → KSP portrait
keyboard*, which restores stock Moonlight behavior.

## Installing

Grab the APK from [Releases](../../releases) and sideload it. The app is a
separate install from official Moonlight (application ID `com.limelight.kspkeyboard`),
so both can coexist; pair it with your host as usual.

## Building

Requires JDK 17+, Android SDK 34, NDK 27.0.12077973.

```
git clone --recursive <this repo>
gradlew assembleNonRootDebug
```

For signed release builds, create `keystore.properties` in the repo root:

```
storeFile=/path/to/your.jks
storePassword=...
keyAlias=...
keyPassword=...
```

then run `gradlew assembleNonRootRelease`.

## Credits & License

All streaming functionality is the work of the
[Moonlight project](https://moonlight-stream.org); this fork only adds the
keyboard/layout layer (see `app/src/main/java/com/limelight/ui/KspKeyboardView.java`
and the layout code in `Game.java`). Kerbal Space Program is a trademark of its
respective owners; this project is not affiliated with or endorsed by them, nor
by the Moonlight project.

Licensed under **GPL-3.0**, same as upstream. Source for all modifications is
in this repository.

## Disclaimer

This software is provided **"as is"**, without warranty of any kind, express or
implied, including but not limited to the warranties of merchantability,
fitness for a particular purpose and noninfringement. In no event shall the
author be liable for any claim, damages or other liability — including but not
limited to data loss, device damage, bricked hardware, lost saves, or exploded
(virtual or otherwise) rockets — arising from the use of this software, its
builds, or its installation instructions. Use at your own risk. See sections 15
and 16 of the [GPL-3.0 license](LICENSE.txt) for the authoritative warranty and
liability terms.
