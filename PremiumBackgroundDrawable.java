package com.example.applock;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

final class PremiumBackgroundDrawable extends Drawable {

    private final Paint paint =
            new Paint(
                    Paint.ANTI_ALIAS_FLAG
            );

    private final int baseColor;
    private final int[] glowColors;
    private final float[] glowX;
    private final float[] glowY;
    private final float[] glowRadius;
    private RadialGradient[] gradients =
            new RadialGradient[0];

    PremiumBackgroundDrawable(
            int baseColor,
            int[] glowColors,
            float[] glowX,
            float[] glowY,
            float[] glowRadius
    ) {

        this.baseColor =
                baseColor;

        this.glowColors =
                glowColors.clone();

        this.glowX =
                glowX.clone();

        this.glowY =
                glowY.clone();

        this.glowRadius =
                glowRadius.clone();
    }

    @Override
    protected void onBoundsChange(
            Rect bounds
    ) {

        super.onBoundsChange(
                bounds
        );

        int width =
                Math.max(
                        1,
                        bounds.width()
                );

        int height =
                Math.max(
                        1,
                        bounds.height()
                );

        float size =
                Math.max(
                        width,
                        height
                );

        gradients =
                new RadialGradient[glowColors.length];

        for (int i = 0;
             i < glowColors.length;
             i++) {

            gradients[i] =
                    new RadialGradient(
                            bounds.left + width * glowX[i],
                            bounds.top + height * glowY[i],
                            size * glowRadius[i],
                            glowColors[i],
                            Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                    );
        }
    }

    @Override
    public void draw(
            Canvas canvas
    ) {

        if (canvas == null) {
            return;
        }

        paint.setShader(
                null
        );

        paint.setStyle(
                Paint.Style.FILL
        );

        paint.setColor(
                baseColor
        );

        canvas.drawRect(
                getBounds(),
                paint
        );

        for (RadialGradient gradient : gradients) {

            paint.setShader(
                    gradient
            );

            canvas.drawRect(
                    getBounds(),
                    paint
            );
        }

        paint.setShader(
                null
        );
    }

    @Override
    public void setAlpha(
            int alpha
    ) {

        paint.setAlpha(
                alpha
        );
    }

    @Override
    public void setColorFilter(
            ColorFilter colorFilter
    ) {

        paint.setColorFilter(
                colorFilter
        );
    }

    @Override
    public int getOpacity() {

        return PixelFormat.OPAQUE;
    }
}
