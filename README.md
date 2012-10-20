AndroidDrawer
=============

A simple but powerful menu drawer implementation. A lot of functionally are based on very [good article][1] by [Cyril Mottier][2] and Facebook mobile application.

Usage
=====

Creating drawer object

```java
Drawer drawer = new Drawer(this, R.layout.drawer_content, getWindow(), 100, 300);
drawer.setFadeDrawer(true);
drawer.setMoveDrawer(true);
```

Opening drawer

```java
drawer.show();
```

BezelSwipe
==========

AndroidDrawer library also provides ability to open drawer by swiping finger from bezel.
To use that functionality override dispatchTouchEvent method in your Activity

```java
@Override
public boolean dispatchTouchEvent(MotionEvent ev)
{
	if (mBezelSwipe == null)
	{
		int dp48 = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
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