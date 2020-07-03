package de.baumann.hhsmoodle;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

public class WebViewWithTouchEvents extends WebView {
    private OnTouchListener onTouchListener;

    public WebViewWithTouchEvents(Context context) {
        super(context);
    }

    public WebViewWithTouchEvents(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WebViewWithTouchEvents(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        onTouchListener.onTouch(this, event);
        return super.onTouchEvent(event);
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        onTouchListener = l;
    }
}
