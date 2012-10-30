AndroidDrawer
=============

A simple but powerful menu drawer implementation. A lot of functionally are based on very [good article][1] by [Cyril Mottier][2] and Facebook mobile application.

You don't need to extend yet another Activity implementation, this simplify integration with bigger applications.

Usage
-

Creating drawer object, second parameter is layout we want to inject into drawer.

```java
mDrawer = Drawer.createLeftDrawer(this, R.layout.drawer_content);
mDrawer.init();
```

Opening drawer

```java
mDrawer.show();
```

Width of drawer is set by default to 48dip in portrait and 40dip in landscape. This can be overriden using method setDrawerWidth(). By providing negative value drawer will expand its width to entire screen minus width provided by parameter.

```java
mDrawer.setDrawerWidth(70f);
```

Orientations can also be set separately

```java
mDrawer.setDrawerWidth(Drawer.ORIENTATION_PORTRAIT, 70f);
mDrawer.setDrawerWidth(Drawer.ORIENTATION_LANDSCAPE, 100f);
```

By default, each time drawer is opened its content is created and disposed when closed, this behaviour can be changed by setting reuse flag

```java
mDrawer.setReuse(true);
```

Drawer can have different animations while opening and closing

```java
mDrawer.setFadeDrawer(true); // this will fade in/out content of drawer
mDrawer.setMoveDrawer(true); // this will create parallax effect
mDrawer.setScaleDrawer(true); // this will create effect of coming content from background
mDrawer.setTransform3dDrawer(true); // this will apply 3d transformation
```
Two last options are very similar to default ICS Launcher effects

Bezel Swipe
-

AndroidDrawer library also provides ability to open drawer by swiping finger from bezel.
To use that functionality override dispatchTouchEvent method in your Activity

```java
@Override
public boolean dispatchTouchEvent(MotionEvent ev)
{
	if (mBezelSwipe == null)
	{
		int dp48 = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics()));
		mBezelSwipe = new BezelSwipe(mDrawer, getWindow(), getSupportActionBar().getHeight(), dp48);
	}

	BezelSwipe.DispatchState returnType = mBezelSwipe.dispatchTouchEvent(ev);
	switch (returnType)
	{
		case RETURN_TRUE:
		{
			return true;
		}
		case RETURN_FALSE:
		{
			return false;
		}
		case FAKE_CANCEL:
		{
			ev.setAction(MotionEvent.ACTION_CANCEL);
			return super.dispatchTouchEvent(ev);
		}
		case CALL_SUPER:
		default:
		{
			return super.dispatchTouchEvent(ev);
		}
	}
}
```


License
=======

    Copyright 2012 Łukasz Milewski, Verdigo Software

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


 [1]: http://android.cyrilmottier.com/?p=658
 [2]: https://github.com/cyrilmottier