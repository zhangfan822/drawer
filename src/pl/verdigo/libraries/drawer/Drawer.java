package pl.verdigo.libraries.drawer;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.FloatMath;
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
import com.actionbarsherlock.internal.nineoldandroids.view.animation.AnimatorProxy;

/**
 * Drawer implementation. TODO create documentation in JavaDoc here.
 * 
 * @author Lukasz Milewski <lukasz.milewski@gmail.com>
 */
public class Drawer implements OnClickListener, OnTouchListener
{

	private static final int DRAWER_CONTENT_MOVE_PROPORTION = 5;

	public static final float LAND_NO_CHANGE = -1f;

	private static final long DEFAULT_DURATION = 250;

	private static final int DRAWER_SHADOW_WIDTH = 12;

	private int mActivityWidth;

	private boolean mAllowCloseOnTouch = true;

	private boolean mAnimationEnabled = true;

	private Drawable mBackground;

	private final Context mContext;

	private FrameLayout mDecorView;

	private int mDeviation = 0;

	private View mDrawer;

	private View mDrawerActivity;

	private ImageView mDrawerClickable;

	private LinearLayout mDrawerContent;

	private DrawerListener mDrawerListener;

	private final float mDrawerMargin;

	private View mDrawerShadow;

	private int mDrawerWidth;

	private boolean mFadeDrawer = false;

	private float mLandDrawerWidth;

	private final int mLayout;

	private boolean mMovable = true;

	private boolean mMoved = false;

	private boolean mMoveDrawer = false;

	private boolean mMovedBeyondMargin = false;

	private int mMovedPosition = 0;

	private boolean mNeedToReinitialize = false;

	private final Window mParentWindow;

	private boolean mReuse = false;

	private boolean mScaleDrawer = false;

	private boolean mTransform3dDrawer = false;

	private boolean mVisible = false;

	/**
	 * Creates {@link Drawer} object.
	 * 
	 * @param context Context
	 * @param layout Layout to inflate into {@link Drawer}
	 * @param parentWindow Window
	 * @param drawerMargin Right margin in portrait mode
	 * @param landDrawerWidth {@link Drawer} width in landscape mode
	 */
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
			long duration = Math.round(DEFAULT_DURATION * (show ? 1F - ratio : ratio));

			return duration >= 0 ? duration : 0;
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

		if (mDrawerListener != null)
		{
			mDrawerListener.onBeforeCancel();
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

		ObjectAnimator anim = ObjectAnimator.ofInt(createDrawerProxy(), "left", start, 0);
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
	 * Cancel (dismiss) {@link Drawer} without animation. This is equivalent to
	 * <pre>
	 * boolean previous = drawer.isAnimationEnabled();
	 * drawer.setAnimationEnabled(false);
	 * drawer.cancel();
	 * drawer.setAnimationEnabled(previous);
	 * </pre>
	 */
	public void cancelWithoutAnimation()
	{
		boolean animationEnabled = mAnimationEnabled;
		mAnimationEnabled = false;

		cancel();

		mAnimationEnabled = animationEnabled;
	}

	/**
	 * Creates DrawerProxy object.
	 * 
	 * @return DrawerProxy object
	 */
	private DrawerProxy createDrawerProxy()
	{
		return new DrawerProxy(mDrawerActivity, mDrawer, mDrawerShadow, mDrawerContent);
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

		return (int) FloatMath.ceil(margin * density);
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

		mDrawerShadow = new LinearLayout(mContext);
		mDrawerShadow.setVisibility(View.GONE);
		mDecorView.addView(mDrawerShadow);

		ImageView shadow = new ImageView(mContext);
		shadow.setLayoutParams(new LinearLayout.LayoutParams(DRAWER_SHADOW_WIDTH, FILL_PARENT));
		shadow.setBackgroundResource(R.drawable.drawer_shadow);
		((LinearLayout) mDrawerShadow).addView(shadow);

		mDrawerClickable = new ImageView(mContext);
		mDrawerClickable.setVisibility(View.GONE);
		mDecorView.addView(mDrawerClickable);

		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(FILL_PARENT, FILL_PARENT);

		mDrawerContent = (LinearLayout) mDrawer.findViewById(R.id.drawer_content);
		mDrawerContent.addView(View.inflate(mContext, mLayout, null), lp);

		updateDrawerWidth();
	}

	/**
	 * Is closing {link Drawer} on touch events allowed. Used primarily with Bezel Swipe.
	 * 
	 * @return Boolean
	 */
	public boolean isAllowCloseOnTouch()
	{
		return mAllowCloseOnTouch;
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
	 * Is fading {@link Drawer} enabled.
	 * 
	 * @return Boolean
	 */
	public boolean isFadeDrawer()
	{
		return mFadeDrawer;
	}

	/**
	 * Is {@link Drawer} movable with touch events.
	 * 
	 * @return Boolean
	 */
	public boolean isMovable()
	{
		return mMovable;
	}

	/**
	 * Is moving content of {@link Drawer} enabled.
	 * 
	 * @return Boolean
	 */
	public boolean isMoveDrawer()
	{
		return mMoveDrawer;
	}

	/**
	 * Is scaling of {@link Drawer} enabled.
	 * 
	 * @return Boolean
	 */
	public boolean isScaleDrawer()
	{
		return mScaleDrawer;
	}

	/**
	 * Is 3d transformation of {@link Drawer} enabled.
	 * 
	 * @return Boolean
	 */
	public boolean isTransform3dDrawer()
	{
		return mTransform3dDrawer;
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
	 * 
	 * @param view Clicked view
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
	 * 
	 * @param view Touched view
	 * @param event Event
	 */
	public boolean onTouch(View view, MotionEvent event)
	{
		if (!mMovable)
		{
			return false;
		}

		if (event.getAction() == MotionEvent.ACTION_UP && isAllowCloseOnTouch())
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
				DrawerProxy proxy = createDrawerProxy();
				proxy.setLeft(mDrawerWidth);
			}

			mDeviation = 0;
			mMoved = false;

			return true;
		}
		if (event.getAction() == MotionEvent.ACTION_UP && !isAllowCloseOnTouch())
		{
			mMovedBeyondMargin = false;
			if (isAnimationEnabled())
			{
				showWithAnimation();
			}
			else
			{
				DrawerProxy proxy = createDrawerProxy();
				proxy.setLeft(mDrawerWidth);
			}

			mDeviation = 0;
			mMoved = false;

			return true;
		}
		else if (event.getAction() == MotionEvent.ACTION_MOVE)
		{
			mMoved = true;
			mMovedPosition = Math.round(event.getRawX() - mDeviation);

			if (mMovedPosition < 0)
			{
				mMovedPosition = 0;
			}

			if (mMovedPosition >= mDrawerWidth)
			{
				mMovedPosition = mDrawerWidth;
			}
			else
			{
				mMovedBeyondMargin = true;
			}

			DrawerProxy proxy = createDrawerProxy();
			proxy.setLeft(mMovedPosition);

			return true;
		}

		return false;
	}

	/**
	 * Removed {@link Drawer} from parent {@link Activity}.
	 */
	public void removeDrawer()
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
		mDrawerShadow.setVisibility(View.GONE);

		if (mReuse)
		{
			ViewGroup.LayoutParams params = mDrawer.getLayoutParams();
			params.width = 0;
			mDrawer.setLayoutParams(params);

			return;
		}

		mDecorView.removeView(mDrawer);
		mDecorView.removeView(mDrawerClickable);
		mDecorView.removeView(mDrawerShadow);

		mNeedToReinitialize = true;
	}

	/**
	 * Sets whether closing {@link Drawer} is available on touch events.
	 * 
	 * @param allowCloseOnTouch true/false
	 */
	public void setAllowCloseOnTouch(boolean allowCloseOnTouch)
	{
		mAllowCloseOnTouch = allowCloseOnTouch;
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
	 * Sets {@link DrawerListener} listener.
	 * 
	 * @param listener New listener
	 */
	public void setDrawerListener(DrawerListener listener)
	{
		mDrawerListener = listener;
	}

	/**
	 * Sets whether {@link Drawer} will fade to black on animation.
	 * 
	 * @param fadeDrawer true/false
	 */
	public void setFadeDrawer(boolean fadeDrawer)
	{
		mFadeDrawer = fadeDrawer;
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
	 * Sets whether content of {@link Drawer} is move during animation.
	 * 
	 * @param moveDrawer true/false
	 */
	public void setMoveDrawer(boolean moveDrawer)
	{
		mMoveDrawer = moveDrawer;
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
	 * Sets whether content of {@link Drawer} is scaled during animation.
	 * 
	 * @param scaleDrawer true/false
	 */
	public void setScaleDrawer(boolean scaleDrawer)
	{
		this.mScaleDrawer = scaleDrawer;
	}

	/**
	 * Sets whether content of {@link Drawer} is 3d transformed during animation.
	 * This method is available from Android 3.0 (API level 11). On lower version
	 * nothing will happen.
	 * 
	 * @param transform3dDrawer true/false
	 */
	public void setTransform3dDrawer(boolean transform3dDrawer)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			this.mTransform3dDrawer = transform3dDrawer;
		}
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
		((ViewGroup) mDrawerActivity.getParent()).setBackgroundResource(android.R.color.black);

		if (isAnimationEnabled())
		{
			showWithAnimation();
		}
		else
		{
			DrawerProxy proxy = createDrawerProxy();
			proxy.setLeft(mActivityWidth - getDrawerMargin());

			updateDrawerClickable();
			updateDrawerShadow();
		}
	}

	void showWithTouch(int deviation)
	{
		if (isVisible())
		{
			return;
		}

		if (mNeedToReinitialize)
		{
			init();
		}

		mMoved = true;
		mMovedPosition = 0;
		mVisible = true;
		mDeviation = deviation;

		mBackground = ((ViewGroup) mDrawerActivity.getParent()).getBackground();
		((ViewGroup) mDrawerActivity.getParent()).setBackgroundResource(android.R.color.black);

		DrawerProxy proxy = createDrawerProxy();
		proxy.setLeft(0);

		updateDrawerClickable();
		updateDrawerShadow();
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

		boolean decelerate = mMoved && !mAllowCloseOnTouch;

		ObjectAnimator anim = ObjectAnimator.ofInt(createDrawerProxy(), "left", start, mDrawerWidth);
		anim.setInterpolator(decelerate ? new DecelerateInterpolator() : new AccelerateInterpolator());
		anim.setDuration(calculateDuration(true));
		anim.start();

		if (mMoved)
		{
			return;
		}

		updateDrawerClickable();
		updateDrawerShadow();
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
	 * Updates shadow - position and visibility.
	 */
	private void updateDrawerShadow()
	{
		FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mDrawerShadow.getLayoutParams();
		lp.gravity = Gravity.FILL_VERTICAL;
		lp.width = 0;

		mDrawerShadow.setLayoutParams(lp);
		mDrawerShadow.setVisibility(View.VISIBLE);
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

	/**
	 * Internal DrawerProxy class to handle animation of {@link Drawer}
	 * 
	 * @author Lukasz Milewski <lukasz.milewski@gmail.com>
	 */
	public class DrawerProxy
	{

		private int mOriginalWidth;

		private View mView;

		private AnimatorProxy mViewAlpha;

		private View mViewShadow;

		private View mViewWidth;
		private View mva;

		public DrawerProxy(View view, View viewWidth, View viewShadow, View alphaView)
		{
			mView = view;
			mViewWidth = viewWidth;
			mViewShadow = viewShadow;
			mOriginalWidth = mDrawerWidth + getDrawerMargin();
			mViewAlpha = AnimatorProxy.wrap(alphaView);
			mva = alphaView;
		}

		public int getLeft()
		{
			return mView.getPaddingLeft();
		}

		public void setAlpha(int position)
		{
			float value = (Float.valueOf(position) / Float.valueOf(mDrawerWidth)) * 0.7f + 0.3f;
			mViewAlpha.setAlpha(value);
		}

		public void setLeft(int left)
		{
			setLeftPadding(mView, left);
			setLeftPadding(mViewShadow, left - 8);

			setWidth(mView, mOriginalWidth + left);
			setWidth(mViewShadow, left);
			setWidth(mViewWidth, left);

			if ((mMoveDrawer || mScaleDrawer) && !mTransform3dDrawer)
			{
				int maxLeft = mDrawerWidth / DRAWER_CONTENT_MOVE_PROPORTION;
				int negativePaddingLeft = -1 * (int) (maxLeft - (Float.valueOf(left) / DRAWER_CONTENT_MOVE_PROPORTION));

				setLeftPadding(mViewWidth, negativePaddingLeft);
			}

			if (mFadeDrawer)
			{
				setAlpha(left);
			}

			if (mScaleDrawer || mTransform3dDrawer )
			{
				setScale(left);
			}

			if (mTransform3dDrawer && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			{
				setTransform3d(left);
			}
		}

		private void setLeftPadding(View view, int left)
		{
			view.setPadding(left, view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
		}

		private void setScale(int position)
		{
			float scale = (Float.valueOf(position) / Float.valueOf(mDrawerWidth)) * 0.2f + 0.8f;
			mViewAlpha.setScaleX(scale);
			mViewAlpha.setScaleY(scale);
		}

		@TargetApi(11)
		private void setTransform3d(int position)
		{
			int maxLeft = Math.round(mDrawerWidth * 0.9f);
			int negativePaddingLeft = -1 * (int) (maxLeft - (Float.valueOf(position) * 0.9f));
			setLeftPadding(mViewWidth, negativePaddingLeft);

			float rotate = (Float.valueOf(position) / Float.valueOf(mDrawerWidth)) * 0.9f + 0.1f;
			mva.setRotationY(-45 + (rotate * 45));
		}

		private void setWidth(View view, int width)
		{
			ViewGroup.LayoutParams params = view.getLayoutParams();
			params.width = width;
			view.setLayoutParams(params);
		}

	}

}
