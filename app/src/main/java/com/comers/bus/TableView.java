package com.comers.bus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class TableView extends View {

    private Paint paint;

    public TableView(Context context) {
        this(context, null);
    }

    public TableView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.RED);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    int width;
    int SWitdh = 1080;
    int lWith = 2;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        width = SWitdh / 8;
        for (int i = 0; i < 7; i++) {
            canvas.drawLine(width / 3 + i * width, 0, width / 3 + i * width + lWith, 900, paint);
        }
    }
}
