package com.example.audioplayer.view; // ← Убедись, что пакет совпадает с папкой!

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

/**
 * Кнопка Play/Pause с тонким контурным индикатором прогресса.
 * - Серое тонкое кольцо (фон)
 * - Белая тонкая линия заполняется по часовой стрелке (прогресс)
 * - Иконка в центре
 * - Все анимации запускаются ТОЛЬКО из UI-потока
 */
public class CircularProgressButton extends View {

    private static final String TAG = "CircularProgressButton";

    // Настройки отрисовки
    private Paint ringPaint;       // Фон кольца (серый, тонкий)
    private Paint progressPaint;   // Прогресс (белый, тонкий)

    // Геометрия
    private RectF oval;
    private float ringWidth = 3f;  // Тонкая линия: 3dp
    private float centerX, centerY;
    private float radius;

    // Состояние
    private float currentAngle = 0f;  // Текущий угол заполнения (0..360)
    private float targetAngle = 0f;   // Целевой угол для анимации
    private boolean isPlaying = false;

    // Анимация
    private ValueAnimator progressAnimator;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Слушатели
    private OnPlayPauseClickListener clickListener;

    public interface OnPlayPauseClickListener {
        void onPlayPauseClick(boolean wasPlaying);
    }

    public CircularProgressButton(Context context) {
        this(context, null);
    }

    public CircularProgressButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircularProgressButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(Color.parseColor("#666666")); // Тёмно-серый для контраста
        ringPaint.setStrokeWidth(ringWidth);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setColor(Color.WHITE);
        progressPaint.setStrokeWidth(ringWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        oval = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;

        float minDim = Math.min(w, h);
        radius = (minDim - ringWidth) / 2f - 2f; // -2px отступ

        oval.set(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawOval(oval, ringPaint);

        if (currentAngle > 0) {
            canvas.drawArc(oval, -90, currentAngle, false, progressPaint);
        }

    }


    public void setProgress(float fraction) {
        fraction = Math.max(0f, Math.min(1f, fraction));
        final float target = fraction * 360f;

        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> {
                currentAngle = target;
                targetAngle = target;
                invalidate();
            });
        } else {
            currentAngle = target;
            targetAngle = target;
            invalidate();
        }
    }


    public void animateProgress(float targetFraction, long duration) {
        targetFraction = Math.max(0f, Math.min(1f, targetFraction));
        final float targetDegrees = targetFraction * 360f;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> startAngleAnimation(targetDegrees, duration));
        } else {
            startAngleAnimation(targetDegrees, duration);
        }
    }


    private void startAngleAnimation(float targetDegrees, long duration) {
        if (progressAnimator != null && progressAnimator.isRunning()) {
            progressAnimator.cancel();
        }

        progressAnimator = ValueAnimator.ofFloat(currentAngle, targetDegrees);
        progressAnimator.setDuration(duration);
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.addUpdateListener(animation -> {
            currentAngle = (float) animation.getAnimatedValue();
            invalidate();
        });
        progressAnimator.start();
    }

    public void resetProgress() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> {
                currentAngle = 0f;
                targetAngle = 0f;
                if (progressAnimator != null) progressAnimator.cancel();
                invalidate();
            });
        } else {
            currentAngle = 0f;
            targetAngle = 0f;
            if (progressAnimator != null) progressAnimator.cancel();
            invalidate();
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (clickListener != null) {
            clickListener.onPlayPauseClick(isPlaying);
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            return performClick();
        }
        return true;
    }

    public void setOnPlayPauseClickListener(OnPlayPauseClickListener listener) {
        this.clickListener = listener;
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
    }
}