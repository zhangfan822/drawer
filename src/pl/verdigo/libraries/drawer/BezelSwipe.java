package pl.verdigo.libraries.drawer;

import android.view.MotionEvent;

public class BezelSwipe
{

	public enum DispatchState
	{
		CALL_SUPER, RETURN_FALSE, RETURN_TRUE;
	}

	private int mActionBarHeight;

	private Drawer mDrawer;

	private boolean mIsBeingDragged = false;

	private int mStartX;

	private int mStartY;

	private int mLeftDragAreaWidth;

	public BezelSwipe(Drawer drawer, int actionBarHeight, int leftDragAreaWidth)
	{
		mDrawer = drawer;
		mActionBarHeight = actionBarHeight;
		mLeftDragAreaWidth = leftDragAreaWidth;
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

		if (!mIsBeingDragged && y < mActionBarHeight)
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
			mDrawer.show();
			return DispatchState.RETURN_FALSE;
		}

		if ((ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) && mIsBeingDragged)
		{
			cancelSwipe();
			mIsBeingDragged = false;
		}

		return DispatchState.CALL_SUPER;
	}

}
