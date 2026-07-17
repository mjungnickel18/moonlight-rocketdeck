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
 * A fixed on-screen keyboard tailored for Kerbal Space Program, meant to be
 * docked below the stream view in portrait mode. Keys act like a physical
 * keyboard: key-down on touch, key-up on release, with full multi-touch
 * support so chords like Shift+W (throttle up while pitching) work.
 */
public class KspKeyboardView extends View {
    // Pseudo keycode for the key that toggles the local IME for free text entry
    private static final int KEY_TOGGLE_IME = -1;

    private static final int STYLE_NORMAL = 0;
    private static final int STYLE_ACCENT = 1;   // flight controls
    private static final int STYLE_THROTTLE = 2;
    private static final int STYLE_DANGER = 3;   // abort
    private static final int STYLE_STAGE = 4;

    public interface Listener {
        void onKey(boolean down, int androidKeyCode);
        void onToggleIme();
    }

    private static class Key {
        final String label;
        final String subLabel;
        final int keyCode;
        final float weight;
        final int style;
        final RectF rect = new RectF();
        int pressCount; // number of pointers currently holding this key

        Key(String label, String subLabel, int keyCode, float weight, int style) {
            this.label = label;
            this.subLabel = subLabel;
            this.keyCode = keyCode;
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

    private double videoAspectRatio = 16.0 / 9.0;
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

        buildLayout();
    }

    private void buildLayout() {
        rows.clear();
        allKeys.clear();

        addRow(
                new Key("ESC", "Pause", KeyEvent.KEYCODE_ESCAPE, 1, STYLE_NORMAL),
                new Key("F5", "QSave", KeyEvent.KEYCODE_F5, 1, STYLE_NORMAL),
                new Key("F9", "QLoad", KeyEvent.KEYCODE_F9, 1, STYLE_NORMAL),
                new Key("M", "Map", KeyEvent.KEYCODE_M, 1, STYLE_NORMAL),
                new Key("V", "Cam", KeyEvent.KEYCODE_V, 1, STYLE_NORMAL),
                new Key("C", "IVA", KeyEvent.KEYCODE_C, 1, STYLE_NORMAL),
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
    }

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

    public void setVideoAspectRatio(double aspectRatio) {
        if (aspectRatio > 0) {
            this.videoAspectRatio = aspectRatio;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Claim the space left over below the video, which is pinned to the
        // top of the parent and letterboxed to the stream's aspect ratio
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        int videoHeight = (int) Math.round(width / videoAspectRatio);
        setMeasuredDimension(width, Math.max(parentHeight - videoHeight, 0));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutKeys(w, h);
    }

    private void layoutKeys(int w, int h) {
        if (w == 0 || h == 0) {
            return;
        }

        float gap = Math.max(2f, w * 0.006f);
        float rowHeight = (h - gap) / rows.size();

        for (int r = 0; r < rows.size(); r++) {
            List<Key> row = rows.get(r);
            float totalWeight = 0;
            for (Key k : row) {
                totalWeight += k.weight;
            }
            float unitWidth = (w - gap) / totalWeight;

            float x = gap;
            float top = gap + r * rowHeight;
            for (Key k : row) {
                float keyWidth = unitWidth * k.weight - gap;
                k.rect.set(x, top, x + keyWidth, top + rowHeight - gap);
                x += unitWidth * k.weight;
            }
        }

        labelPaint.setTextSize(rowHeight * 0.32f);
        subLabelPaint.setTextSize(rowHeight * 0.18f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float radius = Math.max(4f, getWidth() * 0.008f);
        for (Key k : allKeys) {
            keyPaint.setColor(getKeyColor(k));
            canvas.drawRoundRect(k.rect, radius, radius, keyPaint);

            float centerX = k.rect.centerX();
            if (k.subLabel != null) {
                float labelBaseline = k.rect.centerY() + labelPaint.getTextSize() * 0.1f;
                canvas.drawText(k.label, centerX, labelBaseline, labelPaint);
                canvas.drawText(k.subLabel, centerX,
                        labelBaseline + subLabelPaint.getTextSize() * 1.4f, subLabelPaint);
            }
            else {
                canvas.drawText(k.label, centerX,
                        k.rect.centerY() + labelPaint.getTextSize() * 0.35f, labelPaint);
            }
        }
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
                releaseAllPointers();
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

    private void releaseAllPointers() {
        pointerKeyMap.clear();
        for (Key k : allKeys) {
            if (k.pressCount > 0) {
                k.pressCount = 0;
                dispatchKey(k, false);
            }
        }
        invalidate();
    }

    private void dispatchKey(Key k, boolean down) {
        if (listener == null) {
            return;
        }
        if (k.keyCode == KEY_TOGGLE_IME) {
            // Only fire on release so the IME doesn't swallow our up event
            if (!down) {
                listener.onToggleIme();
            }
        }
        else {
            listener.onKey(down, k.keyCode);
        }
    }
}
