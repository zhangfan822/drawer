package pl.verdigo.libraries.drawer;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator.AnimatorListener;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;

public class Drawer implements OnClickListener, OnTouchListener
{

	public static final float LAND_NO_CHANGE = -1f;

	private static final long DEFAULT_DURATION = 350;

	private int mActivityWidth;

	private boolean mAnimationEnabled = true;

	private Drawable mBackground;

	private final Context mContext;

	private FrameLayout mDecorView;

	private View mDrawer;

	private View mDrawerActivity;

	private ImageView mDrawerClickable;

	private LinearLayout mDrawerContent;

	private final float mDrawerMargin;

	private int mDrawerWidth;

	private float mLandDrawerWidth;

	private final int mLayout;

	private boolean mMovable = true;

	private boolean mMoved = false;

	private boolean mMovedBeyondMargin = false;

	private int mMovedPosition = 0;

	private boolean mNeedToReinitialize = false;

	private final Window mParentWindow;

	private boolean mReuse = false;

	private boolean mVisible = false;

	public Drawer(Context context, int layout, Window parentWindow, float drawerMargin, float landDrawerWidth)
	{
		mContext = context;
		mLayout = layout;
		mParentWindow = parentWindow;
		mDrawerMargin = drawerMargin;
		mLandDrawerWidth = landDrawerWidth;

		init();
	}

	/**
	 * Calculates duration of animation. When {@link Drawer} is in state of
	 * moving, duration of animation will be calculated based on the position.
	 * 
	 * @param show Animation for showing/hiding
	 * @return time in milliseconds
	 */
	private long calculateDuration(boolean show)
	{
		if (mMoved)
		{
			float ratio = (float) mMovedPosition / mDrawerWidth;
			return Math.round(DEFAULT_DURATION * (show ? 1F - ratio : ratio));
		}

		return DEFAULT_DURATION;
	}

	/**
	 * Cancel (dismiss) {@link Drawer}. If animation is enabled it will be
	 * played.
	 */
	public void cancel()
	{
		if (!mVisible)
		{
			return;
		}

		mVisible = false;

		mDrawerClickable.setOnClickListener(null);
		mDrawerClickable.setOnTouchListener(null);

		if (mAnimationEnabled)
		{
			cancelWithAnimation();
		}
		else
		{
			removeDrawer();
		}
	}

	/**
	 * Plays cancel animation. It slides {@link Drawer} from right to left. If
	 * drawer is currently moved by touch event, animation will start from
	 * current position and will be appropriately shortened.
	 */
	private void cancelWithAnimation()
	{
		final int start = mMoved ? mMovedPosition : mDrawerWidth;

		ObjectAnimator anim = ObjectAnimator.ofInt(new DrawerProxy(mDrawerActivity, mDrawer), "left", start, 0);
		anim.setInterpolator(new DecelerateInterpolator());
		anim.setDuration(calculateDuration(false));
		anim.addListener(new AnimatorListener()
		{
			public void onAnimationStart(Animator animation)
			{
			}

			public void onAnimationEnd(Animator animation)
			{
				removeDrawer();
			}

			public void onAnimationCancel(Animator animation)
			{
			}

			public void onAnimationRepeat(Animator animation)
			{
			}

		});

		anim.start();
	}

	/**
	 * Returns {@link Drawer} margin. Value provided by developer is in DPI,
	 * therefore it has to be calculated into pixels.
	 * 
	 * @return Drawer margin in pixels
	 */
	private int getDrawerMargin()
	{
		float margin = mDrawerMargin;
		float density = mContext.getResources().getDisplayMetrics().density;

		if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && mLandDrawerWidth != LAND_NO_CHANGE)
		{
			margin = (mActivityWidth / density) - mLandDrawerWidth;
		}

		return (int) Math.ceil(margin * density);
	}

	/**
	 * Initialize {@link Drawer}. Drawer's layout is injected into
	 * most-top-level {@link FrameLayout) possible, this gives us an ability to
	 * move {@link ActionBar}. Clickable {@link ImageView} is also created to
	 * handle click and touch events.
	 */
	private void init()
	{
		mDecorView = (FrameLayout) mParentWindow.getDecorView();
		mDrawerActivity = (ViewGroup) mDecorView.getChildAt(0);

		mActivityWidth = mDrawerActivity.getWidth();

		mDrawer = View.inflate(mContext, R.layout.drawer_placeholder, null);
		mDrawer.setPadding(0, mDrawerActivity.getPaddingTop(), 0, mDrawerActivity.getPaddingBottom());
		mDecorView.addView(mDrawer);

		mDrawerClickable = new ImageView(mContext);
		mDrawerClickable.setVisibility(View.GONE);
		mDecorView.addView(mDrawerClickable);

		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);

		mDrawerContent = (LinearLayout) mDrawer.findViewById(R.id.drawer_content);
		mDrawerContent.addView(View.inflate(mContext, mLayout, null), lp);

		updateDrawerWidth();
	}

	/**
	 * Is animation currently enabled.
	 * 
	 * @return Boolean
	 */
	public boolean isAnimationEnabled()
	{
		return mAnimationEnabled;
	}

	/**
	 * Is {@link Drawer} movable with touch events.
	 */
	public boolean isMovable()
	{
		return mMovable;
	}

	/**
	 * Is drawer currently visible. If it is not visible, internal objects are
	 * destroyed and {@link Drawer} should not be used.
	 * 
	 * @return Boolean
	 */
	public boolean isVisible()
	{
		return mVisible;
	}

	/**
	 * Handles click event.
	 */
	public void onClick(View view)
	{
		if (view == mDrawerClickable)
		{
			cancel();
		}
	}

	/**
	 * Handles touch events. If {@link Drawer} is not movable all touch events
	 * are ignored.
	 */
	public boolean onTouch(View view, MotionEvent event)
	{
		if (!mMovable)
		{
			return false;
		}

		if (event.getAction() == MotionEvent.ACTION_UP)
		{
			int border = mDrawerWidth - (mDrawerWidth / 3);

			if (event.getRawX() < border)
			{
				cancel();
				return true;
			}
			else if (event.getRawX() >= mDrawerWidth && !mMovedBeyondMargin)
			{
				cancel();
				return true;
			}

			mMovedBeyondMargin = false;
			if (mMovedPosition < mDrawerWidth && isAnimationEnabled())
			{
				showWithAnimation();
			}
			else if (!isAnimationEnabled())
			{
				DrawerProxy proxy = new DrawerProxy(mDrawerActivity, mDrawer);
				proxy.setLeft(mDrawerWidth);
			}

			mMoved = false;

			return true;
		}
		else if (event.getAction() == MotionEvent.ACTION_MOVE)
		{
			mMoved = true;

			mMovedPosition = Math.round(event.getRawX());
			if (mMovedPosition >= mDrawerWidth)
			{
				mMovedPosition = mDrawerWidth;
			}
			else
			{
				mMovedBeyondMargin = true;
			}

			DrawerProxy proxy = new DrawerProxy(mDrawerActivity, mDrawer);
			proxy.setLeft(mMovedPosition);

			return true;
		}

		return false;
	}

	/**
	 * Removed {@link Drawer} from parent {@link Activity}.
	 */
	protected void removeDrawer()
	{
		mMovedBeyondMargin = false;
		mMovedPosition = 0;
		
		((FrameLayout) mDrawerActivity.getParent()).setBackgroundDrawable(mBackground);
		
		ViewGroup.LayoutParams lp = ((ViewGroup) mDrawerActivity).getLayoutParams();
		lp.width = -1;
		mDrawerActivity.setLayoutParams(lp);
		
		mDrawerActivity.setPadding(0, mDrawerActivity.getPaddingTop(), mDrawerActivity.getPaddingRight(), mDrawerActivity.getPaddingBottom());
		mDrawerActivity.requestLayout();
		
		mDrawerClickable.setVisibility(View.GONE);

		if (mReuse)
		{
			return;
		}

		mDecorView.removeView(mDrawer);
		mDecorView.removeView(mDrawerClickable);

		mNeedToReinitialize = true;
	}

	/**
	 * Sets whether animation should be enabled or disabled.
	 * 
	 * @param animationEnabled true/false
	 */
	public void setAnimationEnabled(boolean animationEnabled)
	{
		mAnimationEnabled = animationEnabled;
	}

	/**
	 * Sets background {@link Drawable} on {@link Drawer}. This method should be
	 * used instead of background on provided layout.
	 * 
	 * @param drawable Drawable
	 */
	public void setBackgroundDrawable(Drawable drawable)
	{
		mDrawerContent.setBackgroundDrawable(drawable);
		mDrawerContent.setPadding(0, 0, 0, 0);
	}

	/**
	 * Sets background {@link Resource} on {@link Drawer}. This method should be
	 * used instead of background on provided layout.
	 * 
	 * @param drawable Resource
	 */
	public void setBackgroundResource(int drawable)
	{
		mDrawerContent.setBackgroundResource(drawable);
		mDrawerContent.setPadding(0, 0, 0, 0);
	}

	/**
	 * Sets whether {@link Drawer} is movable by touch events.
	 * 
	 * @param movable true/false
	 */
	public void setMovable(boolean movable)
	{
		mMovable = movable;
	}

	/**
	 * Sets whether content of {@link Drawer} will be reused or not.
	 * 
	 * @param reuse true/false
	 */
	public void setReuse(boolean reuse)
	{
		mReuse = reuse;
	}

	/**
	 * Shows {@link Drawer}. If animation is enabled it will be played.
	 */
	public void show()
	{
		if (isVisible())
		{
			return;
		}

		if (mNeedToReinitialize)
		{
			init();
		}

		mMoved = false;
		mMovedPosition = 0;
		mVisible = true;

		mBackground = ((ViewGroup) mDrawerActivity.getParent()).getBackground();
		((ViewGroup) mDrawerActivity.getParent()).setBackgroundColor(android.R.color.black);

		if (isAnimationEnabled())
		{
			showWithAnimation();
		}
		else
		{
			DrawerProxy proxy = new DrawerProxy(mDrawerActivity, mDrawer);
			proxy.setLeft(mActivityWidth - getDrawerMargin());

			updateDrawerClickable();
		}
	}

	/**
	 * Plays show animation. It slides {@link Drawer} from left to right. If
	 * drawer is currently moved by touch event, animation will start from
	 * current position and will be appropriately shortened. If this is first
	 * time, clickable {@link ImageView} will be correctly positioned and
	 * visible.
	 */
	private void showWithAnimation()
	{
		final int start = mMoved ? mMovedPosition : 0;

		ObjectAnimator anim = ObjectAnimator.ofInt(new DrawerProxy(mDrawerActivity, mDrawer), "left", start, mDrawerWidth);
		anim.setInterpolator(new AccelerateInterpolator());
		anim.setDuration(calculateDuration(true));
		anim.start();

		if (mMoved)
		{
			return;
		}

		updateDrawerClickable();
	}

	/**
	 * Updates clickable {@link ImageView} - position and visibility.
	 */
	private void updateDrawerClickable()
	{
		FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mDrawerClickable.getLayoutParams();
		lp.gravity = Gravity.RIGHT | Gravity.FILL_VERTICAL;
		lp.width = getDrawerMargin();

		mDrawerClickable.setLayoutParams(lp);
		mDrawerClickable.setVisibility(View.VISIBLE);
		mDrawerClickable.setClickable(true);
		mDrawerClickable.setOnClickListener(this);
		mDrawerClickable.setOnTouchListener(this);
	}

	/**
	 * Updates {@link Drawer} width. It is based on {@link Activity} minus
	 * margin provided in constructor.
	 */
	private void updateDrawerWidth()
	{
		mDrawer.getLayoutParams().width = 0;
		mDrawer.findViewById(R.id.drawer_content).getLayoutParams().width = mActivityWidth - getDrawerMargin();

		mDrawerWidth = mActivityWidth - getDrawerMargin();
	}

	public class DrawerProxy
	{

		private int mOriginalWidth;

		private View mView;

		private View mViewWidth;

		public DrawerProxy(View view, View viewWidth)
		{
			mView = view;
			mViewWidth = viewWidth;
			mOriginalWidth = mDrawerWidth + getDrawerMargin();
		}

		public int getLeft()
		{
			return mView.getPaddingLeft();
		}

		public void setLeft(int left)
		{
			mView.setPadding(left, mView.getPaddingTop(), mView.getPaddingRight(), mView.getPaddingBottom());

			setWidth(mView, mOriginalWidth + left);
			setWidth(mViewWidth, left);
		}

		public void setWidth(View view, int width)
		{
			ViewGroup.LayoutParams params = view.getLayoutParams();
			params.width = width;
			view.setLayoutParams(params);
		}

	}

}
