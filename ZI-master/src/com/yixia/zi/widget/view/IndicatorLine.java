/*
 * Copyright (C) 2013 YIXIA.COM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yixia.zi.widget.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import com.yixia.zi.R;

public class IndicatorLine extends View {
	
	private static final int AVERAGE = 3;
	private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private int mCurrentItem = 0;
	private int mItemWidth;

	public IndicatorLine(Context context) {
		super(context);
		init();
	}

	public IndicatorLine(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
		int width = displayMetrics.widthPixels;
		mItemWidth = width / AVERAGE;

	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		mPaint.setColor(getResources().getColor(R.color.indicator_color));
		mPaint.setAntiAlias(true);
		final float top = getPaddingTop();
		final float bottom = getHeight() - getPaddingBottom();
		int right = mItemWidth * (mCurrentItem + 1);
		canvas.drawRect(0f, top, right, bottom, mPaint);
	}

	public void setCurrentItem(int currentItem) {
		mCurrentItem = currentItem;
		invalidate();
	}

}
