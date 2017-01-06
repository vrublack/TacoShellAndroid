package com.vrublack.nutrition.tacoshell;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;


public class MacroNutrientChart extends View
{

    private float carbs;
    private float fat;
    private float protein;

    public MacroNutrientChart(Context context)
    {
        super(context);
        init();
    }

    public MacroNutrientChart(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public MacroNutrientChart(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init();
    }

    private void init()
    {
        carbPaint = new Paint();
        carbPaint.setColor(getContext().getResources().getColor(R.color.carbs));
        carbPaint.setAntiAlias(true);
        carbPaint.setStyle(Paint.Style.FILL);
        fatPaint = new Paint();
        fatPaint.setColor(getContext().getResources().getColor(R.color.fat));
        fatPaint.setAntiAlias(true);
        fatPaint.setStyle(Paint.Style.FILL);
        proteinPaint = new Paint();
        proteinPaint.setColor(getContext().getResources().getColor(R.color.protein));
        proteinPaint.setAntiAlias(true);
        proteinPaint.setStyle(Paint.Style.FILL);
        rect = new RectF();
    }

    Paint carbPaint;
    Paint proteinPaint;
    Paint fatPaint;
    RectF rect;

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        //draw background circle anyway
        int left = 0;
        int width = getWidth();
        int top = 0;
        rect.set(left, top, left + width, top + width);
        canvas.drawArc(rect, 0, 360 * carbs, true, carbPaint);
        canvas.drawArc(rect, 360 * carbs, 360 * fat, true, fatPaint);
        canvas.drawArc(rect, 360 * carbs + 360 * fat, 360 * protein, true, proteinPaint);
    }

    /**
     *
     * @param carbs Grams of carbs
     * @param fat Grams of fat
     * @param protein Grams of protein
     */
    public void setBreakdown(float carbs, float fat, float protein)
    {
        carbs *= 4;
        fat *= 9;
        protein *= 4;

        float total = carbs + fat + protein;

        this.carbs = carbs / total;
        this.fat = fat / total;
        this.protein = protein / total;

        invalidate();
    }

    public static int dipToPixels(float dip, Context c)
    {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dip, c.getResources().getDisplayMetrics());
    }

}
