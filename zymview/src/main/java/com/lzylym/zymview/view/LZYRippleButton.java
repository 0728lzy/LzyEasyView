package com.lzylym.zymview.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatTextView;

public class LZYRippleButton extends AppCompatTextView {
    private Paint ripplePaint = new Paint();
    private RadialGradient radialGradient;
    private float radius = -1;
    private float currentX;
    private float currentY;
    private int rippleColor = Color.parseColor("#CCFFFFFF");
    private int rippleSpeed = 10;
    private boolean rippleRepeat = false;
    private int rippleLocation = 0;
    private boolean shouldDrawRipple = false;
    private Handler handler = new Handler();
    private boolean isAnimationRunning = false;
    private boolean isRippleActive = true;
    private float cornerRadius = 0; // 新增圆角半径变量
    private int backgroundColor = Color.TRANSPARENT; // 新增圆角背景颜色变量


    public LZYRippleButton(Context context) {
        this(context, null);
    }

    public LZYRippleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        ripplePaint.setAntiAlias(true);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LZYRippleButton);
        rippleColor = a.getColor(R.styleable.LZYRippleButton_rippleColor, Color.parseColor("#CCFFFFFF"));
        rippleSpeed = a.getInteger(R.styleable.LZYRippleButton_rippleSpeed, 10);
        rippleRepeat = a.getBoolean(R.styleable.LZYRippleButton_rippleRepeat, false);
        rippleLocation = a.getInteger(R.styleable.LZYRippleButton_rippleLocation, 0);
        cornerRadius = a.getDimensionPixelSize(R.styleable.LZYRippleButton_cornerRadius, 0); // 读取圆角半径属性
        backgroundColor = a.getColor(R.styleable.LZYRippleButton_cornerBackgroundColor, Color.RED); // 读取圆角背景颜色属性
        a.recycle();
        setClickable(true);
        setFocusable(true);
    }

    public int getRippleColor() {
        return rippleColor;
    }

    public void setRippleColor(int rippleColor) {
        this.rippleColor = rippleColor;
    }

    public boolean isRippleRepeat() {
        return rippleRepeat;
    }

    public void setRippleRepeat(boolean rippleRepeat) {
        this.rippleRepeat = rippleRepeat;
    }

    public int getRippleSpeed() {
        return rippleSpeed;
    }

    public void setRippleSpeed(int rippleSpeed) {
        this.rippleSpeed = rippleSpeed;
    }

    public int getRippleLocation() {
        return rippleLocation;
    }

    public void setRippleLocation(int rippleLocation) {
        this.rippleLocation = rippleLocation;
    }

    public float getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
        invalidate(); // 确保视图使用新的圆角半径进行更新
    }

    public int getCornerBackgroundColor() {
        return backgroundColor;
    }

    public void setCornerBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        invalidate(); // 确保视图使用新的背景颜色进行更新
    }

    public void cancelCurrentRipple() {
        shouldDrawRipple = false;
        invalidate();
    }

    public void disableRippleEffect() {
        shouldDrawRipple = false;
        isAnimationRunning = false;
        isRippleActive=false;
        handler.removeCallbacksAndMessages(null);
        invalidate();
    }

    public void enableRippleEffect() {
        shouldDrawRipple = true;
        isRippleActive=true;
//        startRippleAnimation();
    }

    public void startRippleEffect(){
        // 创建一个 Handler
        Handler handler = new Handler();
        // 延迟500ms后执行动画
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                radius = 1;
                currentX = getWidth() / 2f;
                currentY = getHeight() / 2f;
                shouldDrawRipple = true;
                startRippleAnimation();
            }
        }, 300); // 500ms 的延迟
        // 如果需要在延迟期间执行其他操作，可以在此添加代码
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isAnimationRunning) {
                    shouldDrawRipple = false;
                    handler.removeCallbacksAndMessages(null);
                }
                if (rippleLocation == 1) {
                    currentX = getWidth() / 2f;
                    currentY = getHeight() / 2f;
                } else {
                    currentX = event.getX();
                    currentY = event.getY();
                }
                radius = 1;
                shouldDrawRipple = true;
                if (isRippleActive)
                    startRippleAnimation();
                performClick();
                break;
            case MotionEvent.ACTION_UP:
                if (!rippleRepeat) {
                    shouldDrawRipple = false;
                    postInvalidate();
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void startRippleAnimation() {
        isAnimationRunning = true;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (shouldDrawRipple && radius > 0 && radius < getMeasuredWidth()) {
                    radialGradient = new RadialGradient(currentX, currentY, radius, Color.TRANSPARENT, rippleColor, Shader.TileMode.CLAMP);
                    ripplePaint.setShader(radialGradient);
                    invalidate(); // 重新绘制
                    radius += 10;
                    handler.postDelayed(this, rippleSpeed);
                } else if (shouldDrawRipple && rippleRepeat) {
                    radius = 1; // 重新开始水波纹效果
                    invalidate();
                    handler.postDelayed(this, rippleSpeed);
                } else {
                    isAnimationRunning = false;
                }
            }
        }, rippleSpeed);
    }

    private Path path = new Path(); // 只需初始化一次Path
    private RectF rect = new RectF(); // 只需初始化一次RectF用于圆角矩形

    @Override
    protected void onDraw(Canvas canvas) {
        // 清除画布
        setBackgroundColor(Color.TRANSPARENT);
        // 绘制圆角矩形背景
        if (cornerRadius > 0) {
            rect.set(0, 0, getWidth(), getHeight());
            float[] radii = {cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius};
            path.reset();
            path.addRoundRect(rect, radii, Path.Direction.CW);
            canvas.clipPath(path);
            // 绘制背景颜色
            canvas.drawColor(backgroundColor);
        }
        // 绘制文本和其他视图
        super.onDraw(canvas);

        if (shouldDrawRipple && radius > 0 && radius < getMeasuredWidth()) {
            if (radialGradient == null) {
                radialGradient = new RadialGradient(currentX, currentY, radius, Color.TRANSPARENT, rippleColor, Shader.TileMode.CLAMP);
            }
            ripplePaint.setShader(radialGradient);
            canvas.drawCircle(currentX, currentY, radius, ripplePaint);
        }
    }
}
