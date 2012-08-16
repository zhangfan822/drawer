package pl.verdigo.libraries.drawer;

import android.view.MotionEvent;
import android.view.Window;
import android.widget.FrameLayout;

public class BezelSwipe
{

	public enum DispatchState
	{
		CALL_SUPER, RETURN_FALSE, RETURN_TRUE;
	}

	private int mActionBarHeight;

	private int mDecorWindowPosition = 0;

	private Drawer mDrawer;

	private boolean mIsBeingDragged = false;

	private int mStartX;

	private int mStartY;

	private int mLeftDragAreaWidth;

	public BezelSwipe(Window parentWindow, Drawer drawer, int actionBarHeight, int leftDragAreaWidth)
	{
		mDrawer = drawer;
		mActionBarHeight = actionBarHeight;
		mLeftDragAreaWidth = leftDragAreaWidth;
		// mDecorWindowPosition = parentWindow.getWindowManager().getDefaultDisplay().getHeight() - parentWindow.getDecorView().getHeight();

		FrameLayout mDecorView = (FrameLayout) parentWindow.getDecorView();
		System.out.println(mDecorView.getHeight());
		System.out.println(mDecorView.getChildAt(0).getHeight());
	}

	private void cancelSwipe()
	{
		mStartX = -1;
		mStartY = -1;
	}

	public DispatchState dispatchTouchEvent(MotionEvent ev)
	{
		int x = Math.round(ev.getX());
		int y = Math.round(ev.getY());

		if (!mIsBeingDragged && y < (mActionBarHeight + mDecorWindowPosition))
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

		if ((ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) && mIsBeingDragged)
		{
			mDrawer.onTouch(null, ev);
			mDrawer.setAllowCloseOnTouch(true);

			cancelSwipe();
			mIsBeingDragged = false;
		}

		return DispatchState.CALL_SUPER;
	}

}
