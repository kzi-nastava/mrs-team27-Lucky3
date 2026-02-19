package com.example.mobile.ui.maps;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import org.osmdroid.views.MapView;

/**
 * A MapView subclass that prevents parent ScrollViews from intercepting
 * touch events when the user interacts with the map. This ensures that
 * panning/scrolling on the map moves the map instead of scrolling the page.
 */
public class TouchableMapView extends MapView {

    public TouchableMapView(Context context) {
        super(context);
    }

    public TouchableMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                // Tell the parent ScrollView to not intercept touch events
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Re-enable parent interception when the user lifts their finger
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.dispatchTouchEvent(event);
    }
}
