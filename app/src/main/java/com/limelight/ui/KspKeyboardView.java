package com.limelight.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * A fixed on-screen keyboard panel tailored for Kerbal Space Program. Keys act
 * like a physical keyboard: key-down on touch, key-up on release, with full
 * multi-touch support so chords like Shift+W (throttle up while pitching) work.
 *
 * Three key layouts cover the game's phases — FLY (flight), VAB (editor) and
 * EVA — switchable via a dedicated key. In portrait, a single panel docks
 * below the video. In landscape, three panels surround it: movement keys
 * left, actions right, and a strip along the bottom.
 */
public class KspKeyboardView extends View {
    public static final int MODE_FLY = 0;
    public static final int MODE_VAB = 1;
    public static final int MODE_EVA = 2;
    public static final int MODE_COUNT = 3;

    // Pseudo keycodes for keys that don't send plain keyboard input
    private static final int KEY_TOGGLE_IME = -1;
    private static final int KEY_MOUSE_LEFT = -2;
    private static final int KEY_MOUSE_RIGHT = -3;
    private static final int KEY_WHEEL_UP = -4;
    private static final int KEY_WHEEL_DOWN = -5;
    private static final int KEY_TOGGLE_FULLSCREEN = -6;
    private static final int KEY_SWITCH_LAYOUT = -7;
    private static final int KEY_MOUSE_MIDDLE = -8;

    // Auto-repeat cadence for the held mouse wheel keys
    private static final long WHEEL_REPEAT_MS = 150;

    // Invisible, untouchable filler used to group keys within a row
    private static Key spacer(float weight) {
        return new Key("", null, KeyEvent.KEYCODE_UNKNOWN, weight, STYLE_NORMAL);
    }

    public interface Listener {
        void onKey(boolean down, int androidKeyCode);
        void onToggleIme();
        void onMouseButton(boolean down, int button); // MotionEvent.BUTTON_-style: 0=left, 1=middle, 2=right
        void onMouseScroll(int direction);
        void onToggleFullscreen();
        void onSwitchLayout();
    }

    public static final int MOUSE_LEFT = 0;
    public static final int MOUSE_MIDDLE = 1;
    public static final int MOUSE_RIGHT = 2;

    private static final int STYLE_NORMAL = 0;
    private static final int STYLE_ACCENT = 1;   // movement/mouse keys
    private static final int STYLE_THROTTLE = 2;
    private static final int STYLE_DANGER = 3;   // abort
    private static final int STYLE_STAGE = 4;

    private static class Key {
        final String label;
        final String subLabel;
        final int keyCode;
        final int modKeyCode; // held modifier sent around keyCode (0 = none)
        final float weight;
        final int style;
        final RectF rect = new RectF();
        float fixedWidthDp; // when > 0, the key gets exactly this width instead of a weight share
        int pressCount; // number of pointers currently holding this key

        Key fixedWidth(float dp) {
            this.fixedWidthDp = dp;
            return this;
        }

        Key(String label, String subLabel, int keyCode, float weight, int style) {
            this(label, subLabel, keyCode, 0, weight, style);
        }

        Key(String label, String subLabel, int keyCode, int modKeyCode, float weight, int style) {
            this.label = label;
            this.subLabel = subLabel;
            this.keyCode = keyCode;
            this.modKeyCode = modKeyCode;
            this.weight = weight;
            this.style = style;
        }
    }

    private final List<List<Key>> rows = new ArrayList<>();
    private final List<Key> allKeys = new ArrayList<>();
    // Maps active pointer IDs to an index in allKeys
    private final SparseIntArray pointerKeyMap = new SparseIntArray();

    private final Paint keyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Listener listener;

    public KspKeyboardView(Context context) {
        super(context);
        initialize();
    }

    public KspKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    private void initialize() {
        setBackgroundColor(0xFF15151C);

        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        subLabelPaint.setColor(0xFF9E9EAE);
        subLabelPaint.setTextAlign(Paint.Align.CENTER);
    }

    private static String switchLabel(int mode) {
        switch (mode) {
            case MODE_VAB: return "VAB ▸";
            case MODE_EVA: return "EVA ▸";
            default: return "FLY ▸";
        }
    }

    private static Key switchKey(int mode, float weight) {
        return new Key(switchLabel(mode), "Layout", KEY_SWITCH_LAYOUT, weight, STYLE_ACCENT);
    }

    // ---------------------------------------------------------------- Portrait

    public static KspKeyboardView createPortrait(Context context, int mode) {
        KspKeyboardView v = new KspKeyboardView(context);
        switch (mode) {
            case MODE_VAB: v.buildPortraitVab(); break;
            case MODE_EVA: v.buildPortraitEva(); break;
            default: v.buildPortraitFly(); break;
        }
        return v;
    }

    private void buildPortraitFly() {
        addRow(
                switchKey(MODE_FLY, 1.2f),
                new Key("ESC", "Pause", KeyEvent.KEYCODE_ESCAPE, 1, STYLE_NORMAL),
                new Key("F5", "QSave", KeyEvent.KEYCODE_F5, 1, STYLE_NORMAL),
                new Key("F9", "QLoad", KeyEvent.KEYCODE_F9, 1, STYLE_NORMAL),
                new Key("M", "Map", KeyEvent.KEYCODE_M, 1, STYLE_NORMAL),
                new Key("V", "Cam", KeyEvent.KEYCODE_V, 1, STYLE_NORMAL),
                new Key("⌨", "Text", KEY_TOGGLE_IME, 1, STYLE_NORMAL),
                new Key("ABORT", "Bksp", KeyEvent.KEYCODE_DEL, 1.5f, STYLE_DANGER)
        );
        addRow(
                new Key("1", null, KeyEvent.KEYCODE_1, 1, STYLE_NORMAL),
                new Key("2", null, KeyEvent.KEYCODE_2, 1, STYLE_NORMAL),
                new Key("3", null, KeyEvent.KEYCODE_3, 1, STYLE_NORMAL),
                new Key("4", null, KeyEvent.KEYCODE_4, 1, STYLE_NORMAL),
                new Key("5", null, KeyEvent.KEYCODE_5, 1, STYLE_NORMAL),
                new Key("6", null, KeyEvent.KEYCODE_6, 1, STYLE_NORMAL),
                new Key("7", null, KeyEvent.KEYCODE_7, 1, STYLE_NORMAL),
                new Key("8", null, KeyEvent.KEYCODE_8, 1, STYLE_NORMAL),
                new Key("9", null, KeyEvent.KEYCODE_9, 1, STYLE_NORMAL),
                new Key("0", null, KeyEvent.KEYCODE_0, 1, STYLE_NORMAL)
        );
        addRow(
                new Key("T", "SAS", KeyEvent.KEYCODE_T, 1, STYLE_NORMAL),
                new Key("R", "RCS", KeyEvent.KEYCODE_R, 1, STYLE_NORMAL),
                new Key("G", "Gear", KeyEvent.KEYCODE_G, 1, STYLE_NORMAL),
                new Key("B", "Brake", KeyEvent.KEYCODE_B, 1, STYLE_NORMAL),
                new Key("U", "Light", KeyEvent.KEYCODE_U, 1, STYLE_NORMAL),
                new Key("F", "Free", KeyEvent.KEYCODE_F, 1, STYLE_NORMAL),
                new Key("◀", "Warp−", KeyEvent.KEYCODE_COMMA, 1, STYLE_NORMAL),
                new Key("▶", "Warp+", KeyEvent.KEYCODE_PERIOD, 1, STYLE_NORMAL),
                new Key("×1", "Warp", KeyEvent.KEYCODE_SLASH, 1, STYLE_NORMAL)
        );
        addRow(
                new Key("Q", "Roll↶", KeyEvent.KEYCODE_Q, 1, STYLE_ACCENT),
                new Key("W", "Pitch↓", KeyEvent.KEYCODE_W, 1, STYLE_ACCENT),
                new Key("E", "Roll↷", KeyEvent.KEYCODE_E, 1, STYLE_ACCENT),
                new Key("I", "Tr↓", KeyEvent.KEYCODE_I, 1, STYLE_NORMAL),
                new Key("H", "Tr Fwd", KeyEvent.KEYCODE_H, 1, STYLE_NORMAL),
                new Key("N", "Tr Back", KeyEvent.KEYCODE_N, 1, STYLE_NORMAL),
                new Key("THR ▲", "Shift", KeyEvent.KEYCODE_SHIFT_LEFT, 1.5f, STYLE_THROTTLE)
        );
        addRow(
                new Key("A", "Yaw←", KeyEvent.KEYCODE_A, 1, STYLE_ACCENT),
                new Key("S", "Pitch↑", KeyEvent.KEYCODE_S, 1, STYLE_ACCENT),
                new Key("D", "Yaw→", KeyEvent.KEYCODE_D, 1, STYLE_ACCENT),
                new Key("J", "Tr←", KeyEvent.KEYCODE_J, 1, STYLE_NORMAL),
                new Key("K", "Tr↑", KeyEvent.KEYCODE_K, 1, STYLE_NORMAL),
                new Key("L", "Tr→", KeyEvent.KEYCODE_L, 1, STYLE_NORMAL),
                new Key("THR ▼", "Ctrl", KeyEvent.KEYCODE_CTRL_LEFT, 1.5f, STYLE_THROTTLE)
        );
        addRow(
                new Key("Z", "Full Thr", KeyEvent.KEYCODE_Z, 1, STYLE_THROTTLE),
                new Key("X", "Cut Thr", KeyEvent.KEYCODE_X, 1, STYLE_THROTTLE),
                new Key("STAGE", "Space", KeyEvent.KEYCODE_SPACE, 3, STYLE_STAGE),
                new Key("FINE", "Caps", KeyEvent.KEYCODE_CAPS_LOCK, 1, STYLE_NORMAL),
                new Key("[ ]", "Vessel", KeyEvent.KEYCODE_RIGHT_BRACKET, 1, STYLE_NORMAL)
        );
        addMouseRow();
    }

    private void buildPortraitVab() {
        addRow(
                switchKey(MODE_VAB, 1.2f),
                new Key("ESC", "Pause", KeyEvent.KEYCODE_ESCAPE, 1, STYLE_NORMAL),
                new Key("UNDO", "Ctrl+Z", KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_CTRL_LEFT, 1, STYLE_NORMAL),
                new Key("REDO", "Ctrl+Y", KeyEvent.KEYCODE_Y, KeyEvent.KEYCODE_CTRL_LEFT, 1, STYLE_NORMAL),
                new Key("⌨", "Search", KEY_TOGGLE_IME, 1, STYLE_NORMAL),
                new Key("DEL", "Part", KeyEvent.KEYCODE_FORWARD_DEL, 1, STYLE_DANGER)
        );
        addRow(
                new Key("1", "Place", KeyEvent.KEYCODE_1, 1, STYLE_NORMAL),
                new Key("2", "Offset", KeyEvent.KEYCODE_2, 1, STYLE_NORMAL),
                new Key("3", "Rotate", KeyEvent.KEYCODE_3, 1, STYLE_NORMAL),
                new Key("4", "Root", KeyEvent.KEYCODE_4, 1, STYLE_NORMAL),
                new Key("F", "Loc/Abs", KeyEvent.KEYCODE_F, 1, STYLE_NORMAL)
        );
        addRow(
                new Key("Q", "Rot↶", KeyEvent.KEYCODE_Q, 1, STYLE_ACCENT),
                new Key("W", "Rot↑", KeyEvent.KEYCODE_W, 1, STYLE_ACCENT),
                new Key("E", "Rot↷", KeyEvent.KEYCODE_E, 1, STYLE_ACCENT),
                new Key("RESET", "Space", KeyEvent.KEYCODE_SPACE, 1.5f, STYLE_STAGE)
        );
        addRow(
                new Key("A", "Rot←", KeyEvent.KEYCODE_A, 1, STYLE_ACCENT),
                new Key("S", "Rot↓", KeyEvent.KEYCODE_S, 1, STYLE_ACCENT),
                new Key("D", "Rot→", KeyEvent.KEYCODE_D, 1, STYLE_ACCENT),
                new Key("FINE", "Shift halt.", KeyEvent.KEYCODE_SHIFT_LEFT, 1.5f, STYLE_THROTTLE)
        );
        addRow(
                new Key("R", "Symmetry", KeyEvent.KEYCODE_R, 1, STYLE_NORMAL),
                new Key("X", "Sym ±", KeyEvent.KEYCODE_X, 1, STYLE_NORMAL),
                new Key("C", "Snap", KeyEvent.KEYCODE_C, 1, STYLE_NORMAL),
                new Key("COPY", "Alt halt.", KeyEvent.KEYCODE_ALT_LEFT, 1, STYLE_THROTTLE),
                new Key("PAN", "MMB halt.", KEY_MOUSE_MIDDLE, 1, STYLE_ACCENT)
        );
        addMouseRow();
    }

    private void buildPortraitEva() {
        addRow(
                switchKey(MODE_EVA, 1.2f),
                new Key("ESC", "Pause", KeyEvent.KEYCODE_ESCAPE, 1, STYLE_NORMAL),
                new Key("F5", "QSave", KeyEvent.KEYCODE_F5, 1, STYLE_NORMAL),
                new Key("F9", "QLoad", KeyEvent.KEYCODE_F9, 1, STYLE_NORMAL),
                new Key("M", "Map", KeyEvent.KEYCODE_M, 1, STYLE_NORMAL),
                new Key("V", "Cam", KeyEvent.KEYCODE_V, 1, STYLE_NORMAL),
                new Key("⌨", "Text", KEY_TOGGLE_IME, 1, STYLE_NORMAL)
        );
        addRow(
                new Key("R", "Jetpack", KeyEvent.KEYCODE_R, 1, STYLE_NORMAL),
                new Key("L", "Lamp", KeyEvent.KEYCODE_L, 1, STYLE_NORMAL),
                new Key("F", "Grab/Use", KeyEvent.KEYCODE_F, 1, STYLE_NORMAL),
                new Key("B", "Board", KeyEvent.KEYCODE_B, 1, STYLE_NORMAL),
                new Key("[ ]", "Next", KeyEvent.KEYCODE_RIGHT_BRACKET, 1, STYLE_NORMAL)
        );
        addRow(
                new Key("Q", "Roll↶", KeyEvent.KEYCODE_Q, 1, STYLE_ACCENT),
                new Key("W", "Vor", KeyEvent.KEYCODE_W, 1, STYLE_ACCENT),
                new Key("E", "Roll↷", KeyEvent.KEYCODE_E, 1, STYLE_ACCENT),
                new Key("JUMP", "Space", KeyEvent.KEYCODE_SPACE, 1.5f, STYLE_STAGE)
        );
        addRow(
                new Key("A", "Links", KeyEvent.KEYCODE_A, 1, STYLE_ACCENT),
                new Key("S", "Zurück", KeyEvent.KEYCODE_S, 1, STYLE_ACCENT),
                new Key("D", "Rechts", KeyEvent.KEYCODE_D, 1, STYLE_ACCENT),
                new Key("RUN/JET ▲", "Shift", KeyEvent.KEYCODE_SHIFT_LEFT, 1.5f, STYLE_THROTTLE)
        );
        addRow(
                new Key("JET ▼", "Ctrl", KeyEvent.KEYCODE_CTRL_LEFT, 1.5f, STYLE_THROTTLE),
                new Key("◀", "Warp−", KeyEvent.KEYCODE_COMMA, 1, STYLE_NORMAL),
                new Key("▶", "Warp+", KeyEvent.KEYCODE_PERIOD, 1, STYLE_NORMAL),
                new Key("×1", "Warp", KeyEvent.KEYCODE_SLASH, 1, STYLE_NORMAL)
        );
        addMouseRow();
    }

    private void addMouseRow() {
        addRow(
                new Key("LMB", "Click", KEY_MOUSE_LEFT, 2, STYLE_ACCENT),
                new Key("⇡", "Wheel", KEY_WHEEL_UP, 1, STYLE_ACCENT),
                new Key("⇣", "Wheel", KEY_WHEEL_DOWN, 1, STYLE_ACCENT),
                new Key("RMB", "Click", KEY_MOUSE_RIGHT, 2, STYLE_ACCENT)
        );
    }

    // --------------------------------------------------------------- Landscape

    public static KspKeyboardView createLandscapeLeft(Context context, int mode) {
        KspKeyboardView v = new KspKeyboardView(context);
        switch (mode) {
            case MODE_VAB:
                v.addRow(
                        new Key("Q", "Rot↶", KeyEvent.KEYCODE_Q, 1, STYLE_ACCENT),
                        new Key("W", "Rot↑", KeyEvent.KEYCODE_W, 1, STYLE_ACCENT),
                        new Key("E", "Rot↷", KeyEvent.KEYCODE_E, 1, STYLE_ACCENT)
                );
                v.addRow(
                        new Key("A", "Rot←", KeyEvent.KEYCODE_A, 1, STYLE_ACCENT),
                        new Key("S", "Rot↓", KeyEvent.KEYCODE_S, 1, STYLE_ACCENT),
                        new Key("D", "Rot→", KeyEvent.KEYCODE_D, 1, STYLE_ACCENT)
                );
                v.addRow(
                        new Key("RESET", "Space", KeyEvent.KEYCODE_SPACE, 1, STYLE_STAGE)
                );
                v.addRow(
                        new Key("FINE", "Shift halt.", KeyEvent.KEYCODE_SHIFT_LEFT, 1, STYLE_THROTTLE)
                );
                v.addRow(
                        new Key("COPY", "Alt halt.", KeyEvent.KEYCODE_ALT_LEFT, 1, STYLE_THROTTLE),
                        new Key("PAN", "MMB", KEY_MOUSE_MIDDLE, 1, STYLE_ACCENT)
                );
                break;
            case MODE_EVA:
                // Movement lives on the RIGHT panel in EVA so the right thumb
                // flies the kerbal; the left thumb works the jetpack throttle
                v.addRow(
                        new Key("R", "Jetpack", KeyEvent.KEYCODE_R, 1, STYLE_NORMAL),
                        new Key("L", "Lamp", KeyEvent.KEYCODE_L, 1, STYLE_NORMAL),
                        new Key("F", "Grab/Use", KeyEvent.KEYCODE_F, 1, STYLE_NORMAL)
                );
                v.addRow(
                        new Key("B", "Board", KeyEvent.KEYCODE_B, 1, STYLE_NORMAL),
                        new Key("[ ]", "Next", KeyEvent.KEYCODE_RIGHT_BRACKET, 1, STYLE_NORMAL),
                        new Key("M", "Map", KeyEvent.KEYCODE_M, 1, STYLE_NORMAL)
                );
                v.addRow(
                        new Key("RUN/JET ▲", "Shift", KeyEvent.KEYCODE_SHIFT_LEFT, 1, STYLE_THROTTLE)
                );
                v.addRow(
                        new Key("JET ▼", "Ctrl", KeyEvent.KEYCODE_CTRL_LEFT, 1, STYLE_THROTTLE)
                );
                v.addRow(
                        new Key("JUMP", "Space", KeyEvent.KEYCODE_SPACE, 1, STYLE_STAGE)
                );
                break;
            default:
                v.addRow(
                        new Key("Q", "Roll↶", KeyEvent.KEYCODE_Q, 1, STYLE_ACCENT),
                        new Key("W", "Pitch↓", KeyEvent.KEYCODE_W, 1, STYLE_ACCENT),
                        new Key("E", "Roll↷", KeyEvent.KEYCODE_E, 1, STYLE_ACCENT)
                );
                v.addRow(
                        new Key("A", "Yaw←", KeyEvent.KEYCODE_A, 1, STYLE_ACCENT),
                        new Key("S", "Pitch↑", KeyEvent.KEYCODE_S, 1, STYLE_ACCENT),
                        new Key("D", "Yaw→", KeyEvent.KEYCODE_D, 1, STYLE_ACCENT)
                );
                v.addRow(
                        new Key("THR ▲", "Shift", KeyEvent.KEYCODE_SHIFT_LEFT, 1, STYLE_THROTTLE)
                );
                v.addRow(
                        new Key("THR ▼", "Ctrl", KeyEvent.KEYCODE_CTRL_LEFT, 1, STYLE_THROTTLE)
                );
                v.addRow(
                        new Key("Z", "Full", KeyEvent.KEYCODE_Z, 1, STYLE_THROTTLE),
                        new Key("X", "Cut", KeyEvent.KEYCODE_X, 1, STYLE_THROTTLE),
                        new Key("FINE", "Caps", KeyEvent.KEYCODE_CAPS_LOCK, 1, STYLE_NORMAL)
                );
                break;
        }
        v.addRow(
                new Key("LMB", "Click", KEY_MOUSE_LEFT, 2, STYLE_ACCENT),
                new Key("⇡", "Wheel", KEY_WHEEL_UP, 1, STYLE_ACCENT)
        );
        return v;
    }

    public static KspKeyboardView createLandscapeRight(Context context, int mode) {
        KspKeyboardView v = new KspKeyboardView(context);
        switch (mode) {
            case MODE_VAB:
                v.addRow(
                        new Key("1", "Place", KeyEvent.KEYCODE_1, 1, STYLE_NORMAL),
                        new Key("2", "Offset", KeyEvent.KEYCODE_2, 1, STYLE_NORMAL),
                        new Key("3", "Rotate", KeyEvent.KEYCODE_3, 1, STYLE_NORMAL)
                );
                v.addRow(
                        new Key("4", "Root", KeyEvent.KEYCODE_4, 1, STYLE_NORMAL),
                        new Key("F", "Loc/Abs", KeyEvent.KEYCODE_F, 1, STYLE_NORMAL),
                        new Key("R", "Symmetry", KeyEvent.KEYCODE_R, 1, STYLE_NORMAL)
                );
                v.addRow(
                        new Key("X", "Sym ±", KeyEvent.KEYCODE_X, 1, STYLE_NORMAL),
                        new Key("C", "Snap", KeyEvent.KEYCODE_C, 1, STYLE_NORMAL),
                        new Key("DEL", "Part", KeyEvent.KEYCODE_FORWARD_DEL, 1, STYLE_DANGER)
                );
                v.addRow(
                        new Key("UNDO", "Ctrl+Z", KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_CTRL_LEFT, 1, STYLE_NORMAL),
                        new Key("REDO", "Ctrl+Y", KeyEvent.KEYCODE_Y, KeyEvent.KEYCODE_CTRL_LEFT, 1, STYLE_NORMAL)
                );
                v.addRow(
                        new Key("ESC", "Pause", KeyEvent.KEYCODE_ESCAPE, 1, STYLE_NORMAL),
                        new Key("⌨", "Search", KEY_TOGGLE_IME, 1, STYLE_NORMAL)
                );
                break;
            case MODE_EVA:
                v.addRow(
                        new Key("Q", "Roll↶", KeyEvent.KEYCODE_Q, 1, STYLE_ACCENT),
                        new Key("W", "Vor", KeyEvent.KEYCODE_W, 1, STYLE_ACCENT),
                        new Key("E", "Roll↷", KeyEvent.KEYCODE_E, 1, STYLE_ACCENT)
                );
                v.addRow(
                        new Key("A", "Links", KeyEvent.KEYCODE_A, 1, STYLE_ACCENT),
                        new Key("S", "Zurück", KeyEvent.KEYCODE_S, 1, STYLE_ACCENT),
                        new Key("D", "Rechts", KeyEvent.KEYCODE_D, 1, STYLE_ACCENT)
                );
                v.addRow(
                        new Key("◀", "Warp−", KeyEvent.KEYCODE_COMMA, 1, STYLE_NORMAL),
                        new Key("▶", "Warp+", KeyEvent.KEYCODE_PERIOD, 1, STYLE_NORMAL),
                        new Key("×1", "Warp", KeyEvent.KEYCODE_SLASH, 1, STYLE_NORMAL)
                );
                v.addRow(
                        new Key("F5", "QSave", KeyEvent.KEYCODE_F5, 1, STYLE_NORMAL),
                        new Key("F9", "QLoad", KeyEvent.KEYCODE_F9, 1, STYLE_NORMAL),
                        new Key("V", "Cam", KeyEvent.KEYCODE_V, 1, STYLE_NORMAL)
                );
                v.addRow(
                        new Key("ESC", "Pause", KeyEvent.KEYCODE_ESCAPE, 1, STYLE_NORMAL),
                        new Key("⌨", "Text", KEY_TOGGLE_IME, 1, STYLE_NORMAL)
                );
                break;
            default:
                v.addRow(
                        new Key("T", "SAS", KeyEvent.KEYCODE_T, 1, STYLE_NORMAL),
                        new Key("R", "RCS", KeyEvent.KEYCODE_R, 1, STYLE_NORMAL),
                        new Key("G", "Gear", KeyEvent.KEYCODE_G, 1, STYLE_NORMAL)
                );
                v.addRow(
                        new Key("B", "Brake", KeyEvent.KEYCODE_B, 1, STYLE_NORMAL),
                        new Key("U", "Light", KeyEvent.KEYCODE_U, 1, STYLE_NORMAL),
                        new Key("M", "Map", KeyEvent.KEYCODE_M, 1, STYLE_NORMAL)
                );
                v.addRow(
                        new Key("◀", "Warp−", KeyEvent.KEYCODE_COMMA, 1, STYLE_NORMAL),
                        new Key("▶", "Warp+", KeyEvent.KEYCODE_PERIOD, 1, STYLE_NORMAL),
                        new Key("×1", "Warp", KeyEvent.KEYCODE_SLASH, 1, STYLE_NORMAL)
                );
                v.addRow(
                        new Key("STAGE", "Space", KeyEvent.KEYCODE_SPACE, 1, STYLE_STAGE)
                );
                v.addRow(
                        new Key("ESC", "Pause", KeyEvent.KEYCODE_ESCAPE, 1, STYLE_NORMAL),
                        new Key("⌨", "Text", KEY_TOGGLE_IME, 1, STYLE_NORMAL),
                        new Key("ABORT", "Bksp", KeyEvent.KEYCODE_DEL, 1, STYLE_DANGER)
                );
                break;
        }
        v.addRow(
                new Key("⇣", "Wheel", KEY_WHEEL_DOWN, 1, STYLE_ACCENT),
                new Key("RMB", "Click", KEY_MOUSE_RIGHT, 2, STYLE_ACCENT)
        );
        return v;
    }

    public static KspKeyboardView createLandscapeBottom(Context context, int mode) {
        KspKeyboardView v = new KspKeyboardView(context);
        switch (mode) {
            case MODE_VAB:
                v.addRow(
                        new Key("⛶", "Full", KEY_TOGGLE_FULLSCREEN, 1, STYLE_ACCENT).fixedWidth(64),
                        switchKey(MODE_VAB, 1).fixedWidth(64),
                        new Key("UNDO", "Ctrl+Z", KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_CTRL_LEFT, 1, STYLE_NORMAL),
                        new Key("REDO", "Ctrl+Y", KeyEvent.KEYCODE_Y, KeyEvent.KEYCODE_CTRL_LEFT, 1, STYLE_NORMAL),
                        new Key("ESC", "Pause", KeyEvent.KEYCODE_ESCAPE, 1, STYLE_NORMAL),
                        new Key("⌨", "Search", KEY_TOGGLE_IME, 1, STYLE_NORMAL)
                );
                break;
            case MODE_EVA:
                v.addRow(
                        new Key("⛶", "Full", KEY_TOGGLE_FULLSCREEN, 1, STYLE_ACCENT).fixedWidth(64),
                        switchKey(MODE_EVA, 1).fixedWidth(64),
                        new Key("F5", "QSave", KeyEvent.KEYCODE_F5, 1, STYLE_NORMAL),
                        new Key("F9", "QLoad", KeyEvent.KEYCODE_F9, 1, STYLE_NORMAL),
                        new Key("M", "Map", KeyEvent.KEYCODE_M, 1, STYLE_NORMAL),
                        new Key("V", "Cam", KeyEvent.KEYCODE_V, 1, STYLE_NORMAL),
                        new Key("ESC", "Pause", KeyEvent.KEYCODE_ESCAPE, 1, STYLE_NORMAL),
                        new Key("⌨", "Text", KEY_TOGGLE_IME, 1, STYLE_NORMAL)
                );
                break;
            default:
                v.addRow(
                        new Key("⛶", "Full", KEY_TOGGLE_FULLSCREEN, 1, STYLE_ACCENT).fixedWidth(64),
                        switchKey(MODE_FLY, 1).fixedWidth(64),
                        new Key("1", null, KeyEvent.KEYCODE_1, 1, STYLE_NORMAL),
                        new Key("2", null, KeyEvent.KEYCODE_2, 1, STYLE_NORMAL),
                        new Key("3", null, KeyEvent.KEYCODE_3, 1, STYLE_NORMAL),
                        new Key("4", null, KeyEvent.KEYCODE_4, 1, STYLE_NORMAL),
                        new Key("5", null, KeyEvent.KEYCODE_5, 1, STYLE_NORMAL),
                        new Key("6", null, KeyEvent.KEYCODE_6, 1, STYLE_NORMAL),
                        new Key("7", null, KeyEvent.KEYCODE_7, 1, STYLE_NORMAL),
                        new Key("8", null, KeyEvent.KEYCODE_8, 1, STYLE_NORMAL),
                        new Key("9", null, KeyEvent.KEYCODE_9, 1, STYLE_NORMAL),
                        new Key("0", null, KeyEvent.KEYCODE_0, 1, STYLE_NORMAL),
                        new Key("F5", "QSave", KeyEvent.KEYCODE_F5, 1, STYLE_NORMAL),
                        new Key("F9", "QLoad", KeyEvent.KEYCODE_F9, 1, STYLE_NORMAL),
                        new Key("F", "Free", KeyEvent.KEYCODE_F, 1, STYLE_NORMAL),
                        new Key("V", "Cam", KeyEvent.KEYCODE_V, 1, STYLE_NORMAL),
                        new Key("C", "IVA", KeyEvent.KEYCODE_C, 1, STYLE_NORMAL),
                        new Key("[ ]", "Vessel", KeyEvent.KEYCODE_RIGHT_BRACKET, 1, STYLE_NORMAL)
                );
                break;
        }
        return v;
    }

    // Two-row bottom strip for screens whose aspect ratio leaves a tall
    // leftover below the video (e.g. 16:10 tablets)
    public static KspKeyboardView createLandscapeBottomTall(Context context, int mode) {
        KspKeyboardView v = createLandscapeBottom(context, mode);
        if (mode == MODE_FLY) {
            // Docking translation, grouped for two-handed grip: fwd/back under
            // the left thumb, the directional cluster under the right thumb
            v.addRow(
                    new Key("H", "Tr Fwd", KeyEvent.KEYCODE_H, 1.2f, STYLE_ACCENT),
                    new Key("N", "Tr Back", KeyEvent.KEYCODE_N, 1.2f, STYLE_ACCENT),
                    spacer(2.2f),
                    new Key("J", "Tr ←", KeyEvent.KEYCODE_J, 1, STYLE_ACCENT),
                    new Key("I", "Tr ↓", KeyEvent.KEYCODE_I, 1, STYLE_ACCENT),
                    new Key("K", "Tr ↑", KeyEvent.KEYCODE_K, 1, STYLE_ACCENT),
                    new Key("L", "Tr →", KeyEvent.KEYCODE_L, 1, STYLE_ACCENT)
            );
        }
        return v;
    }

    // ------------------------------------------------------------------ Layout

    private void addRow(Key... keys) {
        List<Key> row = new ArrayList<>();
        for (Key k : keys) {
            row.add(k);
            allKeys.add(k);
        }
        rows.add(row);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutKeys(w, h);
    }

    private void layoutKeys(int w, int h) {
        if (w == 0 || h == 0 || rows.isEmpty()) {
            return;
        }

        float gap = Math.max(2f, w * 0.006f);
        float rowHeight = (h - gap) / rows.size();
        float density = getResources().getDisplayMetrics().density;

        for (int r = 0; r < rows.size(); r++) {
            List<Key> row = rows.get(r);
            // Fixed-width keys claim their exact size; the rest of the row is
            // shared among the weighted keys
            float totalWeight = 0;
            float fixedTotal = 0;
            for (Key k : row) {
                if (k.fixedWidthDp > 0) {
                    fixedTotal += k.fixedWidthDp * density;
                }
                else {
                    totalWeight += k.weight;
                }
            }
            float unitWidth = totalWeight > 0 ? (w - gap - fixedTotal) / totalWeight : 0;

            float x = gap;
            float top = gap + r * rowHeight;
            for (Key k : row) {
                float slot = (k.fixedWidthDp > 0) ? k.fixedWidthDp * density : unitWidth * k.weight;
                k.rect.set(x, top, x + slot - gap, top + rowHeight - gap);
                x += slot;
            }
        }

        labelPaint.setTextSize(rowHeight * 0.32f);
        subLabelPaint.setTextSize(rowHeight * 0.18f);
    }

    // ----------------------------------------------------------------- Drawing

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float radius = Math.max(4f, getWidth() * 0.008f);
        for (Key k : allKeys) {
            if (k.keyCode == KeyEvent.KEYCODE_UNKNOWN) {
                continue; // spacer
            }
            keyPaint.setColor(getKeyColor(k));
            canvas.drawRoundRect(k.rect, radius, radius, keyPaint);

            float centerX = k.rect.centerX();
            float maxTextWidth = k.rect.width() * 0.92f;
            if (k.subLabel != null) {
                float labelBaseline = k.rect.centerY() + labelPaint.getTextSize() * 0.1f;
                drawFittedText(canvas, k.label, centerX, labelBaseline, labelPaint, maxTextWidth);
                drawFittedText(canvas, k.subLabel, centerX,
                        labelBaseline + subLabelPaint.getTextSize() * 1.4f, subLabelPaint, maxTextWidth);
            }
            else {
                drawFittedText(canvas, k.label, centerX,
                        k.rect.centerY() + labelPaint.getTextSize() * 0.35f, labelPaint, maxTextWidth);
            }
        }
    }

    // Draws text centered at x, shrinking it if it would overflow maxWidth
    private void drawFittedText(Canvas canvas, String text, float x, float baseline,
                                Paint paint, float maxWidth) {
        float originalSize = paint.getTextSize();
        float measured = paint.measureText(text);
        if (measured > maxWidth) {
            paint.setTextSize(originalSize * maxWidth / measured);
        }
        canvas.drawText(text, x, baseline, paint);
        paint.setTextSize(originalSize);
    }

    private int getKeyColor(Key k) {
        if (k.pressCount > 0) {
            return 0xFF5A8DEE;
        }
        switch (k.style) {
            case STYLE_ACCENT:
                return 0xFF3A3A52;
            case STYLE_THROTTLE:
                return 0xFF2F4A38;
            case STYLE_DANGER:
                return 0xFF7A2020;
            case STYLE_STAGE:
                return 0xFF7A5A16;
            default:
                return 0xFF2A2A36;
        }
    }

    // ------------------------------------------------------------------- Touch

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int index = event.getActionIndex();
                handlePointerDown(event.getPointerId(index),
                        event.getX(index), event.getY(index));
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP:
                handlePointerUp(event.getPointerId(event.getActionIndex()));
                return true;
            case MotionEvent.ACTION_UP:
                handlePointerUp(event.getPointerId(0));
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                releaseAllKeys();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void handlePointerDown(int pointerId, float x, float y) {
        for (int i = 0; i < allKeys.size(); i++) {
            Key k = allKeys.get(i);
            if (k.keyCode == KeyEvent.KEYCODE_UNKNOWN) {
                continue; // spacer
            }
            if (k.rect.contains(x, y)) {
                pointerKeyMap.put(pointerId, i);
                k.pressCount++;
                if (k.pressCount == 1) {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    dispatchKey(k, true);
                    invalidate();
                }
                return;
            }
        }
    }

    private void handlePointerUp(int pointerId) {
        int keyIndex = pointerKeyMap.get(pointerId, -1);
        if (keyIndex < 0) {
            return;
        }
        pointerKeyMap.delete(pointerId);

        Key k = allKeys.get(keyIndex);
        if (k.pressCount > 0 && --k.pressCount == 0) {
            dispatchKey(k, false);
            invalidate();
        }
    }

    /**
     * Releases every held key, sending the corresponding up events. Called on
     * touch cancellation and by the host before this panel is removed, so no
     * key is left stuck down on the streaming host.
     */
    public void releaseAllKeys() {
        pointerKeyMap.clear();
        for (Key k : allKeys) {
            if (k.pressCount > 0) {
                k.pressCount = 0;
                dispatchKey(k, false);
            }
        }
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        releaseAllKeys();
        super.onDetachedFromWindow();
    }

    // ---------------------------------------------------------------- Dispatch

    private void dispatchKey(final Key k, boolean down) {
        if (listener == null) {
            return;
        }
        switch (k.keyCode) {
            case KEY_TOGGLE_IME:
                // Only fire on release so the IME doesn't swallow our up event
                if (!down) {
                    listener.onToggleIme();
                }
                break;
            case KEY_TOGGLE_FULLSCREEN:
                // Fire on release; the layout rebuild destroys this view, so
                // there must not be a pending up event for it afterwards
                if (!down) {
                    listener.onToggleFullscreen();
                }
                break;
            case KEY_SWITCH_LAYOUT:
                if (!down) {
                    listener.onSwitchLayout();
                }
                break;
            case KEY_MOUSE_LEFT:
                listener.onMouseButton(down, MOUSE_LEFT);
                break;
            case KEY_MOUSE_MIDDLE:
                listener.onMouseButton(down, MOUSE_MIDDLE);
                break;
            case KEY_MOUSE_RIGHT:
                listener.onMouseButton(down, MOUSE_RIGHT);
                break;
            case KEY_WHEEL_UP:
            case KEY_WHEEL_DOWN:
                final int direction = (k.keyCode == KEY_WHEEL_UP) ? 1 : -1;
                if (down) {
                    listener.onMouseScroll(direction);
                    // Auto-repeat while the key stays held
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (k.pressCount > 0 && listener != null) {
                                listener.onMouseScroll(direction);
                                postDelayed(this, WHEEL_REPEAT_MS);
                            }
                        }
                    }, WHEEL_REPEAT_MS * 2);
                }
                break;
            default:
                if (k.modKeyCode != 0) {
                    // Modifier combo key (e.g. Ctrl+Z): wrap the base key in
                    // the modifier's down/up
                    if (down) {
                        listener.onKey(true, k.modKeyCode);
                        listener.onKey(true, k.keyCode);
                    }
                    else {
                        listener.onKey(false, k.keyCode);
                        listener.onKey(false, k.modKeyCode);
                    }
                }
                else {
                    listener.onKey(down, k.keyCode);
                }
                break;
        }
    }
}
