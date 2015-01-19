package de.ub0r.android.websms;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * ImageView that tries to take the whole width of the screen.
 */
public class FillWidthImageView extends ImageView {

    public FillWidthImageView(Context context) {
        super(context);
    }

    public FillWidthImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FillWidthImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable image = getDrawable();
        if (image != null) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = width * image.getIntrinsicHeight() / image.getIntrinsicWidth();
            setMeasuredDimension(width, height);
        }
        else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}