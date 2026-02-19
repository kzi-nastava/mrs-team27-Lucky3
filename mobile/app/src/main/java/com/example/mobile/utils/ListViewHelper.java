package com.example.mobile.utils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Helper for displaying a ListView inside a ScrollView.
 * 
 * A ListView inside a ScrollView only shows one row because the ScrollView
 * gives it unlimited height, causing it to measure at minimum size.
 * This helper measures all children and sets a fixed total height so
 * every row is visible without scrolling the ListView itself.
 * 
 * Call {@link #setListViewHeightBasedOnChildren(ListView)} after every
 * adapter data change (after {@code notifyDataSetChanged()}).
 */
public class ListViewHelper {

    /**
     * Measures all items in the ListView's adapter and sets the ListView height
     * to their combined height plus dividers. This allows the ListView to display
     * all rows inside a ScrollView.
     *
     * @param listView The ListView whose height should be adjusted.
     */
    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter == null) {
            return;
        }

        int totalHeight = 0;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(
                listView.getWidth(), View.MeasureSpec.UNSPECIFIED);

        for (int i = 0; i < adapter.getCount(); i++) {
            View listItem = adapter.getView(i, null, listView);
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight
                + (listView.getDividerHeight() * (adapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}
