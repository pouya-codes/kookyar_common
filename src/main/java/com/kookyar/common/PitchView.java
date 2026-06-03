package com.kookyar.common;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

@SuppressLint("DrawAllocation")
public class PitchView extends SurfaceView implements Runnable {
    // Tuning parameters
    private int kookPitch;
    private int kookType = 1;
    private String setarType;
    private float centerPitch = 0, currentPitch, midiRef;
    private int laFrequnes;
    
    // Animation and UI state variables
    private int alpha = 0;
    private final int alphaChangeSpeed = 10;
    private int alphaCent = 0;
    private int alphaChangeSpeedCent = 0;
    private final int alphaChangeSpeedCentUnit = 10;
    private boolean firstCenter = true;
    private boolean runned = false;
    private boolean fixed;
    private final float speed = 0.12f; // Increased: faster needle response
    private float lastDX = -1;
    private int width, height;
    
    // Sound intensity gauge variables
    private float soundIntensity = 0f;
    private float targetIntensity = 0f;
    private final float intensitySpeed = 0.1f;
    
    // Performance optimization: Pre-allocated objects
    private final RectF tempRectF = new RectF();
    private final Paint segmentPaint = new Paint();
    private final Paint warningPaint = new Paint();
    private final int[] segmentColors = new int[20]; // Pre-calculated colors
    private long lastAnimationUpdate = 0;
    private float animationPhase = 0f;
    private final Paint texturePaint = new Paint();
    private final Paint pulsePaint = new Paint();
    private final RectF infoBgRect = new RectF();
    
    private final Paint paint = new Paint();
    private final Paint backgroundPaint = new Paint();
    private final Paint needlePaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint arcPaint = new Paint();
    private final Paint gaugePaint = new Paint();
    private final Paint shadowPaint = new Paint();
    
    Thread thread;
    SurfaceHolder surfaceHolder;
    boolean isItOK = false;

    public void pause() {
        isItOK = false;
        while (true) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            break;
        }

    }

    public void resume() {
        isItOK = true;
        thread = new Thread(this);
        thread.start();
    }

    public void setKookType(int kookType) {
        this.kookType = kookType;

    }

    public float getCurrentPitch() {
        return currentPitch;
    }

    @Override
    public void run() {
        try {

            while (isItOK == true) {
                if (!surfaceHolder.getSurface().isValid()) {
                    continue;

                }

                Canvas canvas = surfaceHolder.lockCanvas();
                draw(canvas);
                surfaceHolder.unlockCanvasAndPost(canvas);
                try {
                    // Optimized: reduced frame rate for better battery and performance
                    if (centerPitch == 0) thread.sleep(100);
                    else thread.sleep(33); // ~30 FPS, sufficient for tuner display
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e) {
//            e.print

        }



    }

    private void init() {
        surfaceHolder = getHolder();
        isItOK = true;
        
        // Initialize paint objects with anti-aliasing
        paint.setAntiAlias(true);
        backgroundPaint.setAntiAlias(true);
        needlePaint.setAntiAlias(true);
        textPaint.setAntiAlias(true);
        arcPaint.setAntiAlias(true);
        gaugePaint.setAntiAlias(true);
        shadowPaint.setAntiAlias(true);
        
        // Initialize optimized paint objects
        segmentPaint.setAntiAlias(true);
        warningPaint.setAntiAlias(true);
        warningPaint.setColor(Color.RED);
        warningPaint.setTextAlign(Align.CENTER);
        warningPaint.setFakeBoldText(true);
        
        // Pre-calculate segment colors for performance
        preCalculateSegmentColors();
        
        // Set text paint properties
        textPaint.setTextAlign(Align.CENTER);
        textPaint.setColor(Color.WHITE);
        
        // Set needle paint properties
        needlePaint.setStrokeWidth(8.0f);
        needlePaint.setStrokeCap(Paint.Cap.ROUND);
        
        // Set arc paint properties
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(6.0f);
        
        // Set gauge paint properties
        gaugePaint.setStyle(Paint.Style.STROKE);
        gaugePaint.setStrokeWidth(12.0f);
        gaugePaint.setStrokeCap(Paint.Cap.ROUND);
        
        // Set shadow paint properties
        shadowPaint.setColor(Color.argb(60, 0, 0, 0));
        shadowPaint.setStrokeWidth(10.0f);
        shadowPaint.setStrokeCap(Paint.Cap.ROUND);
    }
    
    // Performance optimization: Pre-calculate segment colors to avoid runtime calculations
    private void preCalculateSegmentColors() {
        for (int i = 0; i < 20; i++) {
            float intensity = (float) i / 20f;
            int red, green, blue;
            
            if (intensity < 0.3f) {
                // Blue to cyan (low levels)
                red = 0;
                green = (int) (255 * intensity / 0.3f);
                blue = 255;
            } else if (intensity < 0.6f) {
                // Cyan to green (medium levels)
                red = 0;
                green = 255;
                blue = (int) (255 * (0.6f - intensity) / 0.3f);
            } else if (intensity < 0.8f) {
                // Green to yellow (good levels)
                red = (int) (255 * (intensity - 0.6f) / 0.2f);
                green = 255;
                blue = 0;
            } else {
                // Yellow to red (high levels)
                red = 255;
                green = (int) (255 * (1.0f - intensity) / 0.2f);
                blue = 0;
            }
            
            segmentColors[i] = Color.argb(200, red, green, blue);
        }
    }

    public boolean getRunned() {
        return runned;
    }

    public void start() {
        thread = new Thread(this);
        thread.start();


    }

    public PitchView(Context context) {
        super(context);
        init();
        start();
    }

    public PitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        start();
    }


    public PitchView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
        start();
    }

    public void setCenterPitch(float centerPitch) {
        alpha = 0;

        if (!firstCenter) {
            this.centerPitch = centerPitch;
        }
        firstCenter = false;


    }

    public void setMidiRef(float midiRef) {
        this.midiRef = midiRef;
    }

    public float getCenterPitch() {
        return centerPitch;

    }

    public void setCurrentPitch(float currentPitch) {
        if (currentPitch <= 12) {
            alphaChangeSpeedCent = -alphaChangeSpeedCentUnit;
        } else {
            alphaChangeSpeedCent = alphaChangeSpeedCentUnit;
        }
        this.currentPitch = currentPitch;
        
        // Update sound intensity based on pitch detection confidence
        // Higher confidence (valid pitch) = higher intensity
        targetIntensity = (currentPitch > 12) ? Math.min(1.0f, currentPitch / 100f) : 0.1f;
    }

    public void setSoundIntensity(float intensity) {
        targetIntensity = Math.max(0f, Math.min(1f, intensity));
    }

    public void setAFrequnse(int Frequnes) {
        this.laFrequnes = Frequnes;

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h / 2, oldw, oldh);
        width = w;
        height = h;
    }


    public void draw(Canvas canvas) {
        super.draw(canvas);
        
        if (centerPitch == 0) {
            // Draw elegant background even when no pitch is detected
            drawBackground(canvas);
            drawPlaceholderInterface(canvas);
            return;
        }

        // Update sound intensity with smooth animation
        updateSoundIntensity();
        
        // Draw enhanced background
        drawBackground(canvas);
        
        float halfWidth = width / 2f;
        float centerY = height * 0.68f; // Further increased: moved much lower for maximum spacing
        float radius = Math.min(width, height) * 0.25f; // Further reduced: smaller radius for more clearance

        // Draw info header with better styling
        drawInfoHeader(canvas);
        
        // Draw sound intensity gauge at the top with improved spacing
        drawSoundIntensityGauge(canvas);

        // Draw main tuning arc with enhanced design
        drawTuningArc(canvas, halfWidth, centerY, radius);

        // Draw precision tick marks
        drawTickMarks(canvas, halfWidth, centerY, radius);

        // Calculate and draw needle
        float dx = (currentPitch - centerPitch) / 2f;
        boolean inTune = (-0.1 < dx && dx < 0.1);
        
        // Update needle position smoothly
        updateNeedlePosition(dx);
        
        // Draw status indicator
        drawStatusIndicator(canvas, dx, inTune);

        // Draw needle with 3D effect
        drawEnhancedNeedle(canvas, halfWidth, centerY, radius, lastDX, inTune);

        // Draw center hub with better design
        drawEnhancedCenterHub(canvas, halfWidth, centerY, inTune && fixed);

        // Draw note information with enhanced styling
        drawEnhancedNoteInfo(canvas, halfWidth, centerY - radius - 70);

        // Draw frequency and cent information with better spacing
        drawFrequencyInfo(canvas, halfWidth, height * 0.98f);
    }

    private void updateSoundIntensity() {
        if (Math.abs(soundIntensity - targetIntensity) > 0.01f) {
            if (soundIntensity < targetIntensity) {
                soundIntensity = Math.min(targetIntensity, soundIntensity + intensitySpeed);
            } else {
                soundIntensity = Math.max(targetIntensity, soundIntensity - intensitySpeed);
            }
        }
    }

    private void drawBackground(Canvas canvas) {
        // Create a more sophisticated gradient background
        LinearGradient gradient = new LinearGradient(0, 0, 0, height,
                Color.argb(255, 25, 25, 40),
                Color.argb(255, 15, 15, 25), Shader.TileMode.CLAMP);
        backgroundPaint.setShader(gradient);
        canvas.drawRect(0, 0, width, height, backgroundPaint);
        backgroundPaint.setShader(null);
        
        // Add subtle texture overlay
        texturePaint.setColor(Color.argb(10, 255, 255, 255));
        for (int i = 0; i < width; i += 4) {
            canvas.drawLine(i, 0, i, height, texturePaint);
        }
    }

    private void drawPlaceholderInterface(Canvas canvas) {
        textPaint.setTextSize(width / 20f);
        textPaint.setColor(Color.argb(120, 255, 255, 255));
        textPaint.setTextAlign(Align.CENTER);
        canvas.drawText("سیم مورد نظر را انتخاب کنید", width / 2f, height / 2f, textPaint);
        
        // Draw a simple pulse animation
        float pulseRadius = (float) (width / 8f + Math.sin(System.currentTimeMillis() / 500.0) * 10);
        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(2.0f);
        pulsePaint.setColor(Color.argb(60, 100, 200, 255));
        canvas.drawCircle(width / 2f, height / 2f, pulseRadius, pulsePaint);
    }

    private void drawInfoHeader(Canvas canvas) {
        // Draw frequency and type info with enhanced styling
        textPaint.setTextSize(width / 28f);
        textPaint.setTextAlign(Align.LEFT);
        
        // Frequency with background
        infoBgRect.set(10, 10, width / 3f, 50);
        backgroundPaint.setColor(Color.argb(80, 0, 0, 0));
        canvas.drawRoundRect(infoBgRect, 15, 15, backgroundPaint);
        
        textPaint.setColor(Color.CYAN);
        canvas.drawText(laFrequnes + " Hz", 20, 35, textPaint);
        
        // Setar type with background
        if (setarType != null && !setarType.isEmpty()) {
            textPaint.setTextAlign(Align.RIGHT);
            infoBgRect.set(width * 2f / 3f, 10, width - 10, 50);
            canvas.drawRoundRect(infoBgRect, 15, 15, backgroundPaint);
            textPaint.setColor(Color.YELLOW);
            canvas.drawText(setarType, width - 20, 35, textPaint);
        }
    }

    private void drawSoundIntensityGauge(Canvas canvas) {
        float gaugeWidth = width * 0.85f;
        float gaugeHeight = 40f;
        float gaugeX = (width - gaugeWidth) / 2f;
        float gaugeY = height * 0.16f; // Further increased: much more separation from header
        
        // Update animation phase only occasionally for performance
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAnimationUpdate > 50) { // Update every 50ms instead of every frame
            animationPhase = (float) Math.sin(currentTime / 300.0) * 0.2f + 0.8f;
            lastAnimationUpdate = currentTime;
        }
        
        // Draw gauge background with rounded corners (reuse RectF)
        tempRectF.set(gaugeX, gaugeY, gaugeX + gaugeWidth, gaugeY + gaugeHeight);
        backgroundPaint.setColor(Color.argb(80, 0, 0, 0));
        canvas.drawRoundRect(tempRectF, 20, 20, backgroundPaint);
        
        // Create heatmap segments with optimized rendering
        int segmentCount = 20;
        float segmentWidth = (gaugeWidth - 4) / segmentCount;
        int currentLevel = (int) (soundIntensity * segmentCount);
        
        // Draw only necessary segments to reduce draw calls
        for (int i = 0; i <= Math.min(currentLevel, segmentCount - 1); i++) {
            float segmentX = gaugeX + 2 + (i * segmentWidth);
            tempRectF.set(segmentX, gaugeY + 2, segmentX + segmentWidth - 2, gaugeY + gaugeHeight - 2);
            
            // Use pre-calculated colors with simple pulse effect
            int baseColor = segmentColors[i];
            int alpha = (int) (Color.alpha(baseColor) * animationPhase);
            segmentPaint.setColor(Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)));
            
            // Remove expensive shadow effects, use simple glow for performance
            segmentPaint.clearShadowLayer();
            
            canvas.drawRoundRect(tempRectF, 6, 6, segmentPaint);
        }
        
        // Draw inactive segments with single color (performance optimization)
        segmentPaint.setColor(Color.argb(40, 100, 100, 100));
        for (int i = Math.max(0, currentLevel + 1); i < segmentCount; i++) {
            float segmentX = gaugeX + 2 + (i * segmentWidth);
            tempRectF.set(segmentX, gaugeY + 2, segmentX + segmentWidth - 2, gaugeY + gaugeHeight - 2);
            canvas.drawRoundRect(tempRectF, 6, 6, segmentPaint);
        }
        
        // Draw peak indicators only when needed
        if (soundIntensity > 0.8f) {
            warningPaint.setTextSize(width / 30f);
            canvas.drawText("⚠", gaugeX + gaugeWidth + 20, gaugeY + gaugeHeight / 2f + 5, warningPaint);
        }
        
        // Draw gauge labels with modern styling
        textPaint.setTextSize(width / 32f);
        textPaint.setColor(Color.argb(180, 255, 255, 255));
        textPaint.setTextAlign(Align.LEFT);
        canvas.drawText("آرام", gaugeX, gaugeY - 15, textPaint);
        textPaint.setTextAlign(Align.RIGHT);
        canvas.drawText("بلند", gaugeX + gaugeWidth, gaugeY - 15, textPaint);
        
        // Draw center title with enhanced styling
        textPaint.setTextSize(width / 28f);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Align.CENTER);
        textPaint.setFakeBoldText(true);
        canvas.drawText("شدت صدا", width / 2f, gaugeY - 25, textPaint);
        textPaint.setFakeBoldText(false);
        
        // Draw dynamic intensity value (reduced string formatting calls)
        textPaint.setTextSize(width / 35f);
        textPaint.setColor(Color.argb(200, 255, 255, 255));
        canvas.drawText(String.format("%.0f%%", soundIntensity * 100), width / 2f, gaugeY + gaugeHeight + 35, textPaint);
    }

    private void drawTuningArc(Canvas canvas, float centerX, float centerY, float radius) {
        RectF arcRect = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        
        // Draw shadow arc
        shadowPaint.setStyle(Paint.Style.STROKE);
        shadowPaint.setStrokeWidth(8.0f);
        RectF shadowRect = new RectF(centerX - radius + 2, centerY - radius + 2, 
                                   centerX + radius + 2, centerY + radius + 2);
        canvas.drawArc(shadowRect, 135, 270, false, shadowPaint);
        
        // Draw background arc
        arcPaint.setColor(Color.argb(60, 255, 255, 255));
        arcPaint.setStrokeWidth(8.0f);
        canvas.drawArc(arcRect, 135, 270, false, arcPaint);
        
        // Draw colored sections for tuning guidance
        arcPaint.setStrokeWidth(6.0f);
        
        // Red zones (out of tune)
        arcPaint.setColor(Color.argb(100, 255, 100, 100));
        canvas.drawArc(arcRect, 135, 90, false, arcPaint); // Left red zone
        canvas.drawArc(arcRect, 315, 90, false, arcPaint); // Right red zone
        
        // Green zone (in tune)
        arcPaint.setColor(Color.argb(120, 100, 255, 100));
        canvas.drawArc(arcRect, 225, 90, false, arcPaint); // Center green zone
    }

    private void drawTickMarks(Canvas canvas, float centerX, float centerY, float radius) {
        Paint tickPaint = new Paint();
        tickPaint.setAntiAlias(true);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        for (int i = -4; i <= 4; i++) {
            double angle = i * Math.PI / 8;
            float startRadius = radius * 0.85f;
            float endRadius = radius * (i % 2 == 0 ? 0.95f : 0.90f);
            
            float startX = centerX + (float) Math.sin(angle) * startRadius;
            float startY = centerY - (float) Math.cos(angle) * startRadius;
            float endX = centerX + (float) Math.sin(angle) * endRadius;
            float endY = centerY - (float) Math.cos(angle) * endRadius;
            
            // Enhanced tick marks with different styles
            if (i == 0) {
                tickPaint.setStrokeWidth(6.0f);
                tickPaint.setColor(Color.GREEN);
                // Draw center mark with circle
                canvas.drawLine(startX, startY, endX, endY, tickPaint);
                tickPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(endX, endY, 4, tickPaint);
                tickPaint.setStyle(Paint.Style.STROKE);
            } else if (Math.abs(i) == 1) {
                tickPaint.setStrokeWidth(4.0f);
                tickPaint.setColor(Color.YELLOW);
                canvas.drawLine(startX, startY, endX, endY, tickPaint);
            } else {
                tickPaint.setStrokeWidth(2.0f);
                tickPaint.setColor(Color.WHITE);
                canvas.drawLine(startX, startY, endX, endY, tickPaint);
            }
        }
    }

    private void updateNeedlePosition(float dx) {
        dx = Math.max(-1, Math.min(1, dx)); // Clamp dx to [-1, 1]
        dx = Math.round(dx * 100) / 100f;
        lastDX = Math.round(lastDX * 100) / 100f;

        if (dx > lastDX && (lastDX + speed) <= dx) {
            lastDX += speed;
            fixed = false;
        } else if (dx < lastDX && (lastDX - speed) >= dx) {
            lastDX -= speed;
            fixed = false;
        } else {
            lastDX = dx;
            fixed = true;
        }
    }

    private void drawStatusIndicator(Canvas canvas, float dx, boolean inTune) {
        textPaint.setTextSize(width / 24f);
        textPaint.setTextAlign(Align.CENTER);
        
        // Draw status background
        RectF statusBg = new RectF(width * 0.1f, height * 0.9f - 20, width * 0.9f, height * 0.9f + 20);
        backgroundPaint.setColor(Color.argb(120, 0, 0, 0));
        canvas.drawRoundRect(statusBg, 20, 20, backgroundPaint);
        
        if (inTune) {
            textPaint.setColor(Color.GREEN);
            canvas.drawText("✓ کوک شده", width / 2f, height * 0.9f + 5, textPaint);
        } else {
            textPaint.setColor(Color.YELLOW);
            String message;
            if (dx < -1) {
                message = (currentPitch == 12) ? "⚡ به سیم ضربه بزنید" : "⬆ سیم را سفت کنید";
            } else {
                message = "⬇ سیم را شل کنید";
            }
            canvas.drawText(message, width / 2f, height * 0.9f + 5, textPaint);
        }
    }

    private void drawEnhancedNeedle(Canvas canvas, float centerX, float centerY, float radius, float dx, boolean inTune) {
        dx = Math.max(-1, Math.min(1, dx)); // Clamp dx to [-1, 1]
        double phi = dx * Math.PI / 4;
        
        float needleLength = radius * 0.8f;
        float needleX = centerX + (float) Math.sin(phi) * needleLength;
        float needleY = centerY - (float) Math.cos(phi) * needleLength;
        
        // Draw needle shadow
        shadowPaint.setStrokeWidth(10.0f);
        canvas.drawLine(centerX + 3, centerY + 3, needleX + 3, needleY + 3, shadowPaint);
        
        // Draw needle with gradient effect
        needlePaint.setStrokeWidth(8.0f);
        if (inTune) {
            needlePaint.setColor(Color.GREEN);
        } else {
            // Color intensity based on how far off-tune
            float intensity = Math.abs(dx);
            int red = (int) (255 * intensity);
            int green = (int) (255 * (1 - intensity));
            needlePaint.setColor(Color.rgb(red, green, 0));
        }
        canvas.drawLine(centerX, centerY, needleX, needleY, needlePaint);
        
        // Draw needle tip
        Paint tipPaint = new Paint();
        tipPaint.setAntiAlias(true);
        tipPaint.setStyle(Paint.Style.FILL);
        tipPaint.setColor(needlePaint.getColor());
        canvas.drawCircle(needleX, needleY, 6, tipPaint);
    }

    private void drawEnhancedCenterHub(Canvas canvas, float centerX, float centerY, boolean tuned) {
        float hubRadius = width / 35f;
        
        // Draw hub shadow
        shadowPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX + 2, centerY + 2, hubRadius + 2, shadowPaint);
        
        // Draw outer ring with gradient
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6.0f);
        paint.setColor(Color.LTGRAY);
        canvas.drawCircle(centerX, centerY, hubRadius + 6, paint);
        
        // Draw middle ring
        paint.setStrokeWidth(3.0f);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, hubRadius + 3, paint);
        
        // Draw center circle with status color
        paint.setStyle(Paint.Style.FILL);
        if (tuned) {
            paint.setColor(Color.GREEN);
        } else {
            // Pulsing yellow when not tuned
            int alpha = (int) (128 + 127 * Math.sin(System.currentTimeMillis() / 300.0));
            paint.setColor(Color.argb(alpha, 255, 255, 0));
        }
        canvas.drawCircle(centerX, centerY, hubRadius, paint);
        
        // Draw center dot
        paint.setColor(Color.BLACK);
        canvas.drawCircle(centerX, centerY, hubRadius / 3f, paint);
    }

    private void drawEnhancedNoteInfo(Canvas canvas, float centerX, float centerY) {
        if (alpha + alphaChangeSpeed <= 255) {
            alpha += alphaChangeSpeed;
        } else {
            alpha = 255;
        }
        
        // Draw note background
        RectF noteBg = new RectF(centerX - width / 4f, centerY - 30, centerX + width / 4f, centerY + 30);
        backgroundPaint.setColor(Color.argb(100, 0, 0, 0));
        canvas.drawRoundRect(noteBg, 15, 15, backgroundPaint);
        
        textPaint.setAlpha(alpha);
        textPaint.setColor(Color.GREEN);
        textPaint.setTextAlign(Align.CENTER);
        textPaint.setTextSize(width / 20f);
        textPaint.setFakeBoldText(true);
        
        String noteName = (kookType != 1) ? drawName2() : drawName();
        canvas.drawText(noteName, centerX, centerY - 5, textPaint);
        
        textPaint.setTextSize(width / 28f);
        textPaint.setColor(Color.CYAN);
        textPaint.setFakeBoldText(false);
        canvas.drawText(String.format("%.1f Hz", midiToFer(centerPitch)), centerX, centerY + 30, textPaint);
    }

    private void drawFrequencyInfo(Canvas canvas, float centerX, float centerY) {
        // Update cent alpha
        if (alphaChangeSpeedCent > 0) {
            alphaCent = Math.min(255, alphaCent + alphaChangeSpeedCent);
        } else if (alphaChangeSpeedCent < 0) {
            alphaCent = Math.max(0, alphaCent + alphaChangeSpeedCent);
        }

        textPaint.setAlpha(alphaCent);
        textPaint.setColor(Color.CYAN);
        textPaint.setTextAlign(Align.CENTER);
        textPaint.setTextSize(width / 28f);
        
        float dif = (currentPitch - centerPitch) * 100;
        dif = Math.round(dif * 10) / 10f;
        String sign = (dif > 0) ? "+" : "";
        
        canvas.drawText(String.format("%.1f Hz / Cent: %s%.1f", 
            midiToFer(currentPitch), sign, dif), centerX, centerY, textPaint);
    }

    private float midiToFer(float midi) {
        double frequens = 440 * Math.pow(2, ((midi - 69) / 12));
        frequens = Math.round(frequens * 100) / 100.0;
        return (float) frequens;
    }

    private String drawName() {
        if (midiRef > 12) {
            String[] noteName = {"دو", "ر بمل", "ر", "می بمل", "می", "فا",
                    "سل بمل", "سل", "لا بمل", "لا", "سی بمل", "سی"};
            String[] positionName = {"اول", "دوم", "سوم", "چهارم", "پنجم",
                    "ششم", "هفتم"};
            float tone = midiRef % 1;
            float midi = (int) midiRef;
            int noteIndex = (int) (midi % 12);
            int position = (int) (midi / 12) - 2;
            String drawName;
            if (tone == 0) {
                drawName = noteName[noteIndex] + " اکتاو " + positionName[position];
            } else {
                if (noteIndex == 0 || noteIndex == 2 || noteIndex == 4
                        || noteIndex == 5 || noteIndex == 7
                        || noteIndex == 9 || noteIndex == 11) {
                    drawName = noteName[(noteIndex)] + " سری " + " اکتاو " + positionName[position];
                } else {
                    drawName = noteName[(noteIndex + 1)] + " کرن " + " اکتاو " + positionName[position];
                }
            }
            return drawName;
        } else {
            return "";
        }
    }

    private String drawName2() {
        String drawName = "";
        if (kookPitch == 1 || kookPitch == 10 || kookPitch == 19) drawName = "می کرن";
        else if (kookPitch == 2 || kookPitch == 11 || kookPitch == 20) drawName = "فا";
        else if (kookPitch == 3 || kookPitch == 12 || kookPitch == 21) drawName = "سل";
        else if (kookPitch == 4 || kookPitch == 13) drawName = "لا کرن";
        else if (kookPitch == 22) drawName = "لا بمل";
        else if (kookPitch == 5 || kookPitch == 14 || kookPitch == 23) drawName = "سی بمل";
        else if (kookPitch == 6 || kookPitch == 15 || kookPitch == 24) drawName = "دو";
        else if (kookPitch == 16 || kookPitch == 25) drawName = "ر";
        else if (kookPitch == 7) drawName = "ر کرن";
        else if (kookPitch == 8 || kookPitch == 17 || kookPitch == 26) drawName = "می بمل";
        else if (kookPitch == 9 || kookPitch == 18 || kookPitch == 27) drawName = "فا";
        else if (kookPitch == 0) drawName = "دو";
        
        String[] positionName = {"اول", "دوم", "سوم", "چهارم", "پنجم",
                "ششم", "هفتم"};
        float midi = (int) midiRef;
        int position = (int) (midi / 12) - 2;
        drawName += " اکتاو " + positionName[position];
        return drawName;
    }

    public void setKookPitch(int kookPitch) {
        this.kookPitch = kookPitch;
    }

    public void setSetarType(String setarType) {
        this.setarType = setarType;
    }
}