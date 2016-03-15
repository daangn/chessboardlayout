package com.jungkai.chessboardlayout;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import java.util.ArrayList;

/**
 *  To make simple Grid like layout with no recycling, no scrolling.
 *  Can use adapter with.
 *
 *  -----------------------
 *  |      |       |      |
 *  |      |       |      |
 *  -----------------------
 *  |      |       |      |
 *  |      |       |      |
 *  -----------------------
 *  |      |       |      |
 *  |      |       |      |
 *  -----------------------
 *
 */
public class ChessBoardLayout extends AdapterView<BaseAdapter> {

    public static final int DEFAULT_COL_COUNT = 2;

    public static final int TOUCH_MODE_NONE = -1;

    public static final int TOUCH_MODE_DOWN = 0;

    public static final int TOUCH_MODE_TAP = 1;

    public static final int TOUCH_MODE_DONE_WAITING = 3;

    private ArrayList<View> scrapViews;

    private SparseIntArray rowHeight;

    private BaseAdapter adapter;

    private int colCount;

    private boolean isDataChanged;

    private int colSpacing;

    private int rowSpacing;

    private int childWidth;

    private int totalWidth;

    private int selectedPosition;

    private View selectedView;

    private int touchMode = TOUCH_MODE_NONE;

    private CheckForLongPress pendingCheckForLongPress;

    private CheckForTap pendingCheckForTap;

    private Runnable touchModeReset;

    private PerformClick performClick;

    private AccessibilityManager accessibilityManager;

    private AdapterDataSetObserver dataSetObserver;

    private BoardItemAccessibilityDelegate accessbilityDeledate;

    private int widthMeasureSpecMode;

    enum FocusDirection {
        FOCUS_UP, FOCUS_DOWN
    }

    public ChessBoardLayout(Context context) {
        super(context);
        init(null);
    }

    public ChessBoardLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ChessBoardLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        accessibilityManager = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);

        setWillNotDraw(true);
        setFocusable(true);

        if (attrs != null) {

            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ChessBoardLayout);

            colCount = a.getInt(R.styleable.ChessBoardLayout_colCount, DEFAULT_COL_COUNT);

            colSpacing = a.getDimensionPixelSize(R.styleable.ChessBoardLayout_colSpacing, 0);

            rowSpacing = a.getDimensionPixelSize(R.styleable.ChessBoardLayout_rowSpacing, 0);

            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        totalWidth = MeasureSpec.getSize(widthMeasureSpec);
        widthMeasureSpecMode = MeasureSpec.getMode(widthMeasureSpec);

        final int horizontalPadding = getPaddingLeft() + getPaddingRight();
        final int verticalPadding = getPaddingTop() + getPaddingBottom();

        final int availableWidth = totalWidth - horizontalPadding;

        if (rowHeight == null) {
            rowHeight = new SparseIntArray();
        } else {
            rowHeight.clear();
        }

        if (adapter == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        if (colCount < 1) {
            throw new IllegalStateException("colCount should be equals or more than 1");
        }

        int totalCount = adapter.getCount();
        if (scrapViews == null || isDataChanged) {
            scrapViews = new ArrayList<View>(totalCount);
        }

        childWidth = (availableWidth - (colSpacing * (colCount - 1))) / colCount;

        int totalHeight = 0;
        int rowIdx = 0;
        int realPosition;
        int childMaxHeightPerRow = Integer.MIN_VALUE;
        View child;
        for (int i = 0; i < totalCount; i += colCount, rowIdx++, childMaxHeightPerRow = Integer.MIN_VALUE) {
            for (int j = 0; j < colCount; j++) {
                realPosition = i + j;
                child = realPosition < totalCount ? obtainView(realPosition) : null;
                if(child != null) {
                    childMaxHeightPerRow = Math.max(childMaxHeightPerRow, child.getMeasuredHeight());
                    if (widthMeasureSpecMode != MeasureSpec.EXACTLY) {
                        childWidth = child.getMeasuredWidth();
                    }
                }
            }
            rowHeight.put(rowIdx, childMaxHeightPerRow);
            totalHeight += childMaxHeightPerRow;
        }

        if (totalCount > 0) {
            int rowCount = scrapViews.size() / colCount + ((scrapViews.size() % colCount == 0) ? 0 : 1);
            totalHeight = totalHeight + (rowCount - 1) * rowSpacing;
        }

        int totalWidth = MeasureSpec.getSize(widthMeasureSpec);
        int totalHeightWithPadding = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMeasureSpecMode != MeasureSpec.EXACTLY) {
            totalWidth = childWidth * colCount + (colSpacing * (colCount - 1)) + horizontalPadding;
        }

        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
            totalHeightWithPadding = verticalPadding + totalHeight; //setMeasuredDimension(totalWidth, verticalPadding + totalHeight);
        }

        setMeasuredDimension(totalWidth, totalHeightWithPadding);
    }

    private View obtainView(int position) {
        View child;
        if (canUseScrapView(position) || !isDataChanged) {
            child = scrapViews.get(position);
        } else {

            child = adapter.getView(position, null, this);

            LayoutParams p = getChildLayoutParams(child);
            measureChild(child, p);

            scrapViews.add(position, child);
        }

        if (ViewCompat.getImportantForAccessibility(child) == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            ViewCompat.setImportantForAccessibility(child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        if (accessibilityManager.isEnabled()) {
            accessbilityDeledate = new BoardItemAccessibilityDelegate();
            ViewCompat.setAccessibilityDelegate(child, accessbilityDeledate);
        }

        return child;
    }

    private boolean canUseScrapView(int position) {
        return scrapViews.size() != 0 && scrapViews.size() > position;
    }

    private LayoutParams getChildLayoutParams(View child) {
        LayoutParams p = child.getLayoutParams();

        if (p == null) {
            p = generateDefaultLayoutParams();
            child.setLayoutParams(p);
        }
        return p;
    }

    private void measureChild(View child, LayoutParams p) {
        int childHeightSpec = getChildMeasureSpec(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.height);
        int childWidthSpec;
        if (widthMeasureSpecMode == MeasureSpec.EXACTLY) {
            childWidthSpec = getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY), 0, p.width);
        } else {
            childWidthSpec = getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.width);
        }

        child.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (adapter != null && adapter.getCount() > 0 && isDataChanged) {

            final int paddingStart = getPaddingLeft();
            final int paddingTop = getPaddingTop();

            int rowIdx = -1;

            removeAllViewsInLayout();

            for (int i = 0; i < adapter.getCount(); i++) {
                View child = obtainView(i);

                int leftIdx = i % colCount;

                if (leftIdx == 0) {
                    rowIdx++;
                }

                LayoutParams p = child.getLayoutParams();

                if (p == null) {
                    p = generateDefaultLayoutParams();
                }

                addViewInLayout(child, i, p, true);

                int start;

                if (LayoutUtils.isLayoutRTL(getContext())) {
                    start = totalWidth - paddingStart - ((leftIdx + 1) * child.getMeasuredWidth());
                } else {
                    start = paddingStart + (leftIdx * child.getMeasuredWidth());
                }

                int topWithoutPadding = 0;

                for (int j = 0; j < rowIdx; j++) {
                    topWithoutPadding += rowHeight.get(j);
                }

                int top = paddingTop + topWithoutPadding;

                if (leftIdx > 0) {
                    if (LayoutUtils.isLayoutRTL(getContext())) {
                        start -= colSpacing * leftIdx;
                    } else {
                        start += colSpacing * leftIdx;
                    }
                }

                if (rowIdx > 0) {
                    top += rowSpacing * rowIdx;
                }
                child.layout(start, top, start + child.getMeasuredWidth(), top + rowHeight.get(rowIdx));
            }

            isDataChanged = false;
        }
    }

    public void setRowSpacing(int rowSpacing) {
        boolean needRequest = this.rowSpacing != rowSpacing;
        this.rowSpacing = rowSpacing;
        requestLayoutIfNeeded(needRequest);
    }

    public int getRowSpacing() {
        return this.rowSpacing;
    }

    public void setColSpacing(int colSpacing) {
        boolean needRequest = this.colSpacing != colSpacing;
        this.colSpacing = colSpacing;
        requestLayoutIfNeeded(needRequest);
    }

    public int getColSpacing() {
        return this.colSpacing;
    }

    public void setColCount(int colCount) {
        boolean needRequest = this.colCount != colCount;
        this.colCount = colCount;
        requestLayoutIfNeeded(needRequest);
    }

    public int getColCount() {
        return this.colCount;
    }

    protected void requestLayoutIfNeeded(boolean updateFlag) {
        if (updateFlag) {
            isDataChanged = true;
            requestLayout();
        }
    }

    @Override
    public View getSelectedView() {
        return (scrapViews != null && selectedPosition >= 0 && selectedPosition < scrapViews.size()) ? scrapViews.get(selectedPosition) : null;
    }

    @Override
    public void setSelection(int position) {
        selectedPosition = position;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    public BaseAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void setAdapter(BaseAdapter adapter) {
        if (this.adapter != null && dataSetObserver != null) {
            this.adapter.unregisterDataSetObserver(dataSetObserver);
        }
        if (scrapViews != null) {
            scrapViews.clear();
        }

        this.adapter = adapter;

        selectedPosition = INVALID_POSITION;
        touchMode = TOUCH_MODE_NONE;

        if (this.adapter != null) {

            dataSetObserver = new AdapterDataSetObserver();

            this.adapter.registerDataSetObserver(dataSetObserver);

            isDataChanged = true;

            setSelection(0);
        }

        requestLayout();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (scrapViews != null) {
            scrapViews.clear();
            scrapViews = null;
        }

        if (adapter != null && dataSetObserver != null) {
            adapter.unregisterDataSetObserver(dataSetObserver);
            dataSetObserver = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (adapter != null && dataSetObserver == null) {
            dataSetObserver = new AdapterDataSetObserver();
            adapter.registerDataSetObserver(dataSetObserver);

            isDataChanged = true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return isClickable() || isLongClickable();
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {

                selectedPosition = getPositionFromCoord((int) event.getX(), (int) event.getY());
                selectedView = getChildAt(selectedPosition);
                if (selectedPosition != INVALID_POSITION) {

                    clearSelectionState();

                    if ((selectedPosition >= 0) && getAdapter().isEnabled(selectedPosition)) {

                        touchMode = TOUCH_MODE_DOWN;

                        if (pendingCheckForTap == null) {
                            pendingCheckForTap = new CheckForTap();
                        }
                        postDelayed(pendingCheckForTap, ViewConfiguration.getTapTimeout());
                    }
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                switch (touchMode) {
                    case TOUCH_MODE_DOWN:
                    case TOUCH_MODE_TAP:
                    case TOUCH_MODE_DONE_WAITING:
                        final float x = event.getX();
                        final float y = event.getY();
                        if (getPositionFromCoord((int) x, (int) y) != selectedPosition) {
                            if (selectedView != null) {
                                selectedView.setPressed(false);
                                touchMode = TOUCH_MODE_NONE;
                                removeCallbacks(touchMode == TOUCH_MODE_DOWN ?
                                        pendingCheckForTap : pendingCheckForLongPress);
                            }

                        }
                }
                break;
            }

            case MotionEvent.ACTION_UP: {
                switch (touchMode) {
                    case TOUCH_MODE_DOWN:
                    case TOUCH_MODE_TAP:
                    case TOUCH_MODE_DONE_WAITING:
                        if (selectedView != null) {
                            if (touchMode != TOUCH_MODE_DOWN) {
                                selectedView.setPressed(false);
                            }

                            if (performClick == null) {
                                performClick = new PerformClick();
                            }

                            final PerformClick performClick = this.performClick;
                            performClick.rememberWindowAttachCount();

                            if (touchMode == TOUCH_MODE_DOWN || touchMode == TOUCH_MODE_TAP) {
                                removeCallbacks(touchMode == TOUCH_MODE_DOWN ?
                                        pendingCheckForTap : pendingCheckForLongPress);

                                touchMode = TOUCH_MODE_TAP;

                                selectedView.setPressed(true);

                                if (touchModeReset != null) {
                                    removeCallbacks(touchModeReset);
                                }

                                touchModeReset = new Runnable() {
                                    @Override
                                    public void run() {
                                        touchModeReset = null;
                                        touchMode = TOUCH_MODE_NONE;
                                        selectedView.setPressed(false);
                                        if (getWindowToken() != null) {
                                            performClick.run();
                                        }
                                    }
                                };
                                postDelayed(touchModeReset,
                                        ViewConfiguration.getPressedStateDuration());
                            } else {
                                performClick.run();
                            }
                        }

                        touchMode = TOUCH_MODE_NONE;
                        break;
                }
            }

            case MotionEvent.ACTION_CANCEL: {
                if (touchMode != TOUCH_MODE_NONE) {
                    touchMode = TOUCH_MODE_NONE;
                    final View child = this.getChildAt(selectedPosition);
                    if (child != null) {
                        child.setPressed(false);
                    }
                    removeCallbacks(pendingCheckForLongPress);
                }

                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                break;
            }
        }

        return true;
    }

    final class CheckForTap implements Runnable {
        @Override
        public void run() {
            if (touchMode == TOUCH_MODE_DOWN) {
                touchMode = TOUCH_MODE_TAP;
                final View child = getChildAt(selectedPosition);
                if (child != null && !child.hasFocusable()) {
                    child.setPressed(true);
                    refreshDrawableState();

                    final boolean longClickable = isLongClickable();

                    if (longClickable) {
                        if (pendingCheckForLongPress == null) {
                            pendingCheckForLongPress = new CheckForLongPress();
                        }
                        pendingCheckForLongPress.rememberWindowAttachCount();
                        postDelayed(pendingCheckForLongPress, ViewConfiguration.getLongPressTimeout());
                    } else {
                        touchMode = TOUCH_MODE_DONE_WAITING;
                    }
                } else {
                    touchMode = TOUCH_MODE_DONE_WAITING;
                }
            }
        }


    }

    final class CheckForLongPress extends WindowRunnable implements Runnable {
        @Override
        public void run() {
            final View child = getChildAt(selectedPosition);
            if (child != null) {
                final long longPressId = adapter.getItemId(selectedPosition);
                boolean handled = false;
                if (sameWindow())
                    handled = performLongPress(child, selectedPosition, longPressId);

                if (handled) {
                    touchMode = TOUCH_MODE_NONE;
                    child.setPressed(false);
                } else {
                    touchMode = TOUCH_MODE_DONE_WAITING;
                }
            }
        }
    }

    final class PerformClick extends WindowRunnable implements Runnable {

        @Override
        public void run() {
            if (adapter != null && selectedPosition != INVALID_POSITION && sameWindow()) {
                final View view = getChildAt(selectedPosition);
                if (view != null) {
                    performItemClick(view, selectedPosition, adapter.getItemId(selectedPosition));
                }
            }
        }
    }


    private boolean performLongPress(final View child,
                                     final int longPressPosition, final long longPressId) {

        boolean handled = false;
        if (getOnItemLongClickListener() != null) {
            handled = getOnItemLongClickListener().onItemLongClick(this, child,
                    longPressPosition, longPressId);
        }

        if (handled) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
        return handled;
    }

    private int getPositionFromCoord(int x, int y) {
        Rect viewFrame = new Rect();

        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                child.getHitRect(viewFrame);
                if (viewFrame.contains(x, y)) {
                    return i;
                }
            }
        }
        return INVALID_POSITION;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isConfirmKey(keyCode)) {
            if (!isEnabled()) {
                return true;
            }
            if (isClickable() &&
                    selectedPosition >= 0 && adapter != null && selectedPosition < adapter.getCount()) {

                final View view = getChildAt(selectedPosition);
                if (view != null) {
                    performItemClick(view, selectedPosition, adapter.getItemId(selectedPosition));
                    view.setPressed(false);
                }
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    public static final boolean isConfirmKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (getChildCount() <= 0) {
            return super.onKeyDown(keyCode, event);
        }

        boolean handled = false;
        int action = event.getAction();

        if (action != KeyEvent.ACTION_UP) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    handled = updateSelection(FocusDirection.FOCUS_DOWN);
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    handled = updateSelection(FocusDirection.FOCUS_UP);
                    break;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    handled = keyPressed();
                    break;
            }
        }

        if (handled) {
            return true;
        }

        switch (action) {
            case KeyEvent.ACTION_DOWN:
                return super.onKeyDown(keyCode, event);
            case KeyEvent.ACTION_UP:
                return super.onKeyUp(keyCode, event);
            default:
                return false;
        }
    }

    public boolean keyPressed() {
        if (!isEnabled() || !isClickable()) {
            return false;
        }

        if (isFocused()) {

            final View v = getChildAt(selectedPosition);

            if (v != null) {
                if (v.hasFocusable()) return false;
                v.setPressed(true);
            }

            final boolean longClickable = isLongClickable();

            if (longClickable) {
                if (pendingCheckForLongPress == null) {
                    pendingCheckForLongPress = new CheckForLongPress();
                }
                pendingCheckForLongPress.rememberWindowAttachCount();
                postDelayed(pendingCheckForLongPress, ViewConfiguration.getLongPressTimeout());
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean updateSelection(FocusDirection direction) {
        getChildAt(selectedPosition).setSelected(false);

        if (direction == FocusDirection.FOCUS_UP) {
            if (selectedPosition == 0) {
                return false;
            }
            selectedPosition--;
        } else {
            if (selectedPosition == getChildCount() - 1) {
                return false;
            }
            selectedPosition++;
        }

        getChildAt(selectedPosition).setSelected(true);
        return true;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        if (getChildCount() <= 0) {
            return;
        }

        if (gainFocus && !isInTouchMode()) {
            View selectedView = getChildAt(selectedPosition);
            selectedView.setSelected(true);
        } else {
            clearSelectionState();
        }
    }

    private void clearSelectionState() {
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setSelected(false);
        }
    }

    class AdapterDataSetObserver extends DataSetObserver {

        @Override
        public void onChanged() {
            isDataChanged = true;
            requestLayout();
        }

        @Override
        public void onInvalidated() {
            isDataChanged = true;
            selectedPosition = INVALID_POSITION;
            requestLayout();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (getChildCount() > 0) {
            scrapViews.clear();
            isDataChanged = true;
        }
    }

    class BoardItemAccessibilityDelegate extends AccessibilityDelegateCompat {
        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);

            int position = getPositionForView(host);
            onInitializeAccessibilityNodeInfoForItem(position, info);
        }

        @Override
        public boolean performAccessibilityAction(View host, int action, Bundle args) {
            if (super.performAccessibilityAction(host, action, args)) {
                return true;
            }

            final int position = getPositionForView(host);
            final BaseAdapter adapter = getAdapter();

            if ((position == INVALID_POSITION) || (adapter == null)) {
                return false;
            }

            if (!isEnabled() || !adapter.isEnabled(position)) {
                return false;
            }

            final long id = getItemIdAtPosition(position);

            switch (action) {
                case AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION: {
                    if (selectedPosition == position) {
                        setSelection(INVALID_POSITION);
                        return true;
                    }
                } return false;
                case AccessibilityNodeInfoCompat.ACTION_SELECT: {
                    if (selectedPosition != position) {
                        setSelection(position);
                        return true;
                    }
                } return false;
                case AccessibilityNodeInfoCompat.ACTION_CLICK: {
                    if (isClickable()) {
                        return performItemClick(host, position, id);
                    }
                } return false;
                case AccessibilityNodeInfoCompat.ACTION_LONG_CLICK: {
                    if (isLongClickable()) {
                        return performLongPress(host, position, id);
                    }
                } return false;
            }

            return false;
        }

    }

    public void onInitializeAccessibilityNodeInfoForItem(
            int position, AccessibilityNodeInfoCompat info) {
        if (position == INVALID_POSITION || adapter == null) {
            return;
        }

        if (!isEnabled() || !adapter.isEnabled(position)) {
            info.setEnabled(false);
            return;
        }

        if (position == selectedPosition) {
            info.setSelected(true);
            info.addAction(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
        } else {
            info.addAction(AccessibilityNodeInfoCompat.ACTION_SELECT);
        }

        if (isClickable()) {
            info.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            info.setClickable(true);
        }

        if (isLongClickable()) {
            info.addAction(AccessibilityNodeInfoCompat.ACTION_LONG_CLICK);
            info.setLongClickable(true);
        }
    }

    private class WindowRunnable {
        private int mOriginalAttachCount;

        public void rememberWindowAttachCount() {
            mOriginalAttachCount = getWindowAttachCount();
        }

        public boolean sameWindow() {
            return getWindowAttachCount() == mOriginalAttachCount;
        }
    }

}
