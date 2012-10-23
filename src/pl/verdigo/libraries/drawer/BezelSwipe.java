package pl.verdigo.libraries.drawer;

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.Window;

/**
 * Bezel Swipe helper class.
 * 
 * @author Lukasz Milewski <lukasz.milewski@gmail.com>
 */
public class BezelSwipe
{

	public enum DispatchState
	{
		CALL_SUPER, FAKE_CANCEL, RETURN_FALSE, RETURN_TRUE;
	}

	private int mIgnoredTopHeight;

	private Drawer mDrawer;

	private boolean mIsBeingDragged = false;

	private int mStartX;

	private int mStartY;

	private int mLeftDragAreaWidth;

	/**
	 * Creates BezelSwipe object.
	 * 
	 * @param drawer Drawer
	 * @param window Window
	 * @param ignoredTopHeight Ignored height
	 * @param leftDragAreaWidth Left drag area
	 */
	public BezelSwipe(Drawer drawer, Window window, int ignoredTopHeight, int leftDragAreaWidth)
	{
		mDrawer = drawer;
		mIgnoredTopHeight = ignoredTopHeight;
		mLeftDragAreaWidth = leftDragAreaWidth;

		updateNotificationBarHeight(window);
	}

	private void updateNotificationBarHeight(Window window)
	{
		Rect rect = new Rect();
		window.getDecorView().getWindowVisibleDisplayFrame(rect);

		int notificationHeight = rect.top;
		mIgnoredTopHeight += notificationHeight;
	}

	private void cancelSwipe()
	{
		mStartX = -1;
		mStartY = -1;
	}

	/**
	 * Wrapper for dispatching touch events.
	 * 
	 * @param ev Motion event
	 * @return Return state to original method
	 */
	public DispatchState dispatchTouchEvent(MotionEvent ev)
	{
		int x = Math.round(ev.getX());
		int y = Math.round(ev.getY());

		if (!mIsBeingDragged && y < mIgnoredTopHeight)
		{
			return DispatchState.CALL_SUPER;
		}

		if (ev.getAction() == MotionEvent.ACTION_DOWN)
		{
			mIsBeingDragged = false;
			if (x < mLeftDragAreaWidth)
			{
				mStartX = x;
				mStartY = y;
			}
			else
			{
				mStartX = -1;
				mStartY = -1;
			}

			return DispatchState.CALL_SUPER;
		}

		if (ev.getAction() == MotionEvent.ACTION_MOVE && mStartX >= 0 && !mIsBeingDragged)
		{
			if (Math.abs(y - mStartY) > mLeftDragAreaWidth)
			{
				cancelSwipe();
				return DispatchState.CALL_SUPER;
			}

			if (x - mStartX >= mLeftDragAreaWidth)
			{
				mIsBeingDragged = true;
			}

			return DispatchState.CALL_SUPER;
		}

		if (ev.getAction() == MotionEvent.ACTION_MOVE && mIsBeingDragged)
		{
			mDrawer.isMovable();
			mDrawer.setAllowCloseOnTouch(false);
			mDrawer.showWithTouch(x);
			mDrawer.onTouch(null, ev);

			return DispatchState.RETURN_FALSE;
		}

		if (ev.getAction() == MotionEvent.ACTION_UP && mIsBeingDragged)
		{
			mDrawer.finishShowing();
		}

		if ((ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) && mIsBeingDragged)
		{
			mDrawer.onTouch(null, ev);
			mDrawer.setAllowCloseOnTouch(true);

			cancelSwipe();
			mIsBeingDragged = false;

			return DispatchState.FAKE_CANCEL;
		}

		return DispatchState.CALL_SUPER;
	}

}
