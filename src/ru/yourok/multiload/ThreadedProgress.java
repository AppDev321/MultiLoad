package ru.yourok.multiload;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import ru.yourok.multiload.service.HttpDownLoadManager;
import ru.yourok.multiload.service.utils.Range;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 31.07.12
 * Time: 13:28
 */
public class ThreadedProgress extends View
{
    private HttpDownLoadManager manager;
    private Rect mRect;
    private GradientDrawable mDrawable;
    private String progress;
    private String detail;
    float fontSize;

    public ThreadedProgress(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        manager = null;
        progress = "";
        detail = "";
        mDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF000080, 0xFF1E90FF, 0xFF000080});
        float GESTURE_THRESHOLD_DP = 13.0f;
        final float scale = getResources().getDisplayMetrics().density;
        fontSize = (int) (GESTURE_THRESHOLD_DP * scale + 0.5f);
    }

    public void setParams(HttpDownLoadManager manager, String progress, String detail)
    {
        this.manager = manager;
        this.progress = progress;
        this.detail = detail;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        if (manager != null)
        {
            ArrayList<Range> ranges = manager.getRanges();
            long fileSize = manager.getContentSize();
            if (!ranges.isEmpty() && fileSize > 0)
            {
                mRect = new Rect(0, 0, width, height);
                mDrawable.setBounds(mRect);
                canvas.save();
                canvas.translate(0, 0);
                mDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                mDrawable.draw(canvas);

                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setARGB(0xff, 0, 0, 0);
                paint.setStyle(Paint.Style.FILL);

                for (Range range : ranges)
                {
                    long start = range.offset * width / fileSize;
                    long end = range.end * width / fileSize;
                    if (start == end)
                        end++;
                    canvas.drawRect(new Rect((int) start, 0, (int) end, height), paint);
                }
            }
        }
        int color = Color.LTGRAY;

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        paint.setColor(color);
        paint.setTextSize(fontSize);
        canvas.drawText(progress, 2, fontSize, paint);
        canvas.drawText(detail, 2, fontSize * 2 + 4, paint);

        canvas.restore();

        invalidate();
    }
}
