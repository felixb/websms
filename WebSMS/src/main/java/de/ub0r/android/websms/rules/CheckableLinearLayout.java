package de.ub0r.android.websms.rules;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

/**
 * LinearLayout that implements a Checkable interface.
 */
public class CheckableLinearLayout
        extends LinearLayout
        implements Checkable {

    private boolean mChecked = false;

    private static final int[] mCheckedStateSet = {
            android.R.attr.state_checked,
    };


    public CheckableLinearLayout(Context context)
    {
        super(context);
    }

    public CheckableLinearLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @SuppressLint("NewApi")
    public CheckableLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        mChecked = checked;
        refreshDrawableState();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, mCheckedStateSet);
        }
        return drawableState;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

}