package com.my.scroller;

import java.util.ArrayList;
import java.util.List;

import android.R.color;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * @author VitaliyT
 * vitaliyt@interlink-ua.com
 * Nov 18 2010
 */

public class CustomSpinner extends ScrollView {
	
	private static final int START_CORRECTION_SCROLLING = 2;
	private static final int SCROLL_BY_STEP = 3;

	private final int LAST_STEP_TRUE = 1;
	private final int LAST_STEP_FALSE = 0;

	private final int DURATION_OF_CONTROLLER = 30;
	private final int PADDING_VERTICAL = 20;
	private final int PAUSE_COEFICIENT = 4;
	final double SCROLLING_STEP_DX = 0.9;
	
	private final int NO_DEFINED_BACKGROUND = -100;
	
	private int rowHeight;
	private int footerBackground;
	private int headerBackground;
	private int prevChangePos;
	private int lastChangePos;
	private long lastChangeTime;
	private int currentChild;
	private int pixelsNeedToScroll;
	private boolean wasChildChange;
	private boolean isTouched;
	private boolean isScrollingToClosestCenter;

	private List<Integer> childBackground = new ArrayList<Integer>();
	private CustomSpinnerListener listener;
	private ScrollController controller;
	private Handler handler = new SpinnerEventsHandler();
	private List<TextView> views;

	public CustomSpinner(Context context) {
		super(context);
		initCustomSpinner(context, null);
		sendMessageCorrectSpin();
	}

	public CustomSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);
		initCustomSpinner(context, attrs);
		sendMessageCorrectSpin();
	}

	private void initCustomSpinner(Context context, AttributeSet attrs) {
		wasChildChange = false;
		isTouched = false;
		isScrollingToClosestCenter = false;
		
		if (attrs == null) return;
		
		TypedArray attrsArray = context.obtainStyledAttributes(attrs, R.styleable.CustomSpinner);
		int[] customBackgrounds = new int[] { R.styleable.CustomSpinner_child_background, R.styleable.CustomSpinner_child_background_2,
				R.styleable.CustomSpinner_child_background_3 };
		for (int id : customBackgrounds) {
			int bg = attrsArray.getResourceId(id, NO_DEFINED_BACKGROUND);
			if (bg != NO_DEFINED_BACKGROUND) {
				childBackground.add(bg);
			}
		}
		headerBackground = attrsArray.getResourceId(R.styleable.CustomSpinner_header_background, NO_DEFINED_BACKGROUND);
		footerBackground = attrsArray.getResourceId(R.styleable.CustomSpinner_footer_background, NO_DEFINED_BACKGROUND);
	}

	@Override
	protected void onScrollChanged(int x, int y, int oldX, int oldY) {
		super.onScrollChanged(x, y, oldX, oldY);
		lastChangeTime = System.currentTimeMillis();
		lastChangePos = y;
	}

	private class SpinnerEventsHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			switch (msg.what) {
				case START_CORRECTION_SCROLLING:
					isScrollingToClosestCenter = true;
					break;
				case SCROLL_BY_STEP:
					if (msg.arg1 != 0) {
						smoothScrollBy(0, msg.arg1);
					}
					if (msg.arg2 == LAST_STEP_TRUE) {
						isScrollingToClosestCenter = false;
						prevChangePos = lastChangePos;
					}
					break;
				default:
					return;
			}
		}
	}

	private class ScrollController implements Runnable {
		public boolean isRun = false;

		ScrollController() {
			isRun = true;
		}

		@Override
		public void run() {
			while (isRun) {
				long timeNow = System.currentTimeMillis();
				boolean canModify = (timeNow - lastChangeTime) > DURATION_OF_CONTROLLER * PAUSE_COEFICIENT;
				if (!isScrollingToClosestCenter) {
					setCurrentChild();
					if (canModify && !isTouched && needUpdate() && getScrollDx() != 0) {
						prevChangePos = lastChangePos;
						pixelsNeedToScroll = getScrollDx();
						sendMessageCorrectSpin();
					}
				} else {
					pixelsNeedToScroll = getScrollDx();
					sendMessageScrollByStep(getCurrentStep(), getLastStep());
				}
				try {
					Thread.sleep(DURATION_OF_CONTROLLER);
				}
				catch (InterruptedException e) {
					isRun = false;
				}
			}
		}
	}

	private void setCurrentChild() {
		int newCurrentChild = lastChangePos / rowHeight + 1;
		wasChildChange = (currentChild != newCurrentChild);
		currentChild = newCurrentChild;
		if (wasChildChange && listener != null) {
			listener.notifyScrollChanged();
		}
	}

	private int getCurrentStep() {
		int step = (int) Math.sqrt(Math.abs(pixelsNeedToScroll * SCROLLING_STEP_DX));
		step = (pixelsNeedToScroll != 0 && step == 0) ? 1 : step;
		int direction = (pixelsNeedToScroll < 0) ? -1 : 1;
		step *= direction;
		return step;
	}

	private int getLastStep() {
		return (pixelsNeedToScroll == 0) ? LAST_STEP_TRUE : LAST_STEP_FALSE;
	}

	/**
	 * Starts an inner components's controller.
	 * <p>
	 *(recommended to call in <b>onResume()</b> in a parent <b>Activity</b>)
	 * <p>
	 * You need to call this method if you want child TextViews will be centered
	 * after scrolling, and listener which you set via
	 * <b>setCurrentChildChangedListener()</b> to call <b>onScrollChanged()</b>
	 * after centring, only after calling this method, methods
	 * <b>getCurrentTextViewId()</b> and <b>getCurrentTextView()</b> will return
	 * a centered child.
	 */
	public void startController() {
		controller = new ScrollController();
		(new Thread(controller)).start();
	}

	/**
	 * Stops an inner components's controller.
	 * <p>
	 * You need to call this method (recommended to call in <b>onStop()</b> in
	 * parent <b>Activity</b>) if was called <b>startController()</b> before.
	 */
	public void stopController() {
		if (controller != null)
			controller.isRun = false;
		controller = null;
	}

	private void sendMessageCorrectSpin() {
		final Message msg = Message.obtain();
		msg.what = START_CORRECTION_SCROLLING;
		handler.sendMessageDelayed(msg, DURATION_OF_CONTROLLER*5);
	}

	private void sendMessageScrollByStep(int dx, int lastStep) {
		final Message msg = Message.obtain();
		msg.arg1 = dx;
		msg.arg2 = lastStep;
		msg.what = SCROLL_BY_STEP;
		handler.sendMessage(msg);
	}

	private boolean needUpdate() {
		return (prevChangePos != lastChangePos);
	}

	private int getScrollDx() {
		int res = lastChangePos;
		res = lastChangePos % rowHeight;

		if (res > (rowHeight / 2 + PADDING_VERTICAL)) {
			res = rowHeight - res;
		} else {
			res *= -1;
		}

		return res + PADDING_VERTICAL;
	}

	/**
	 * @return integer id of TextView which is displayed on center. If
	 *         <b>startController()</b> was not called it always will return 0.
	 * @see <b>setViews()</b>, <b>startController()</b>
	 */
	public int getCurrentTextViewId() {
		return currentChild - 1;
	}

	/**
	 * @return TextView which is displayed on center. If
	 *         <b>startController()</b> was not called it always will return
	 *         first TextView.
	 * @see <b>setViews()</b>, <b>startController()</b>
	 */
	public TextView getCurrentTextView() {
		return views.get(currentChild - 1);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				isTouched = true;
				break;

			case MotionEvent.ACTION_UP:
				isTouched = false;
				break;
		}
		return super.onTouchEvent(ev);
	}

	/**
	 * Method add a list of TextViews to a CustomSpinner. You can get a centered
	 * TextView via <b>getCurrentTextView()</b>.
	 * 
	 * @see <b>startController()</b> <b>getCurrentTextView()</b>
	 */
	public void setViews(List<TextView> views, Context ctx) {
		this.removeAllViews();
		this.views = views;
		rowHeight = this.getLayoutParams().height / 3;
		LinearLayout layout = new LinearLayout(ctx);
		layout.setGravity(Gravity.CENTER_HORIZONTAL);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setLayoutParams(getMyLayoutParams());

		this.addView(layout);
		addPadding(layout, ctx);
		addHeader(layout, ctx);
		if (views.size() == 0) {
			addEmptyChild(layout, ctx);
		} else {
			for (int i = 0; i < views.size(); i++) {
				setCustomBackground(ctx, views.get(i));
				views.get(i).setHeight(rowHeight);
				layout.addView(views.get(i), getMyLayoutParams());
			}
		}
		addFooter(layout, ctx);
		addPadding(layout, ctx);
	}
	
	public List<TextView>getViews(){
		return views;
	}

	private void setCustomBackground(Context ctx, TextView view) {
		if (childBackground.size() > 0) {
			int newBg = childBackground.get(0);
			childBackground.remove(0);
			childBackground.add(newBg);
			view.setBackgroundDrawable(ctx.getResources().getDrawable(newBg));
		}
	}

	private LinearLayout.LayoutParams getMyLayoutParams() {
		return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 
				LinearLayout.LayoutParams.WRAP_CONTENT);
	}

	private void addHeader(LinearLayout layout, Context ctx) {
		TextView header = new TextView(ctx);
		header.setHeight(rowHeight);
		if (headerBackground != NO_DEFINED_BACKGROUND) {
			final Drawable d = getResources().getDrawable(headerBackground);
			header.setBackgroundDrawable(d);
		}
		layout.addView(header, getMyLayoutParams());
	}

	private void addFooter(LinearLayout layout, Context ctx) {
		TextView footer = new TextView(ctx);
		footer.setHeight(rowHeight);
		if (footerBackground != NO_DEFINED_BACKGROUND) {
			final Drawable d = getResources().getDrawable(footerBackground);
			footer.setBackgroundDrawable(d);
		}
		layout.addView(footer, getMyLayoutParams());
	}
	
	private void addEmptyChild(LinearLayout layout, Context ctx) {
		TextView child = new TextView(ctx);
		child.setHeight(rowHeight);
		setCustomBackground(ctx, child);
		layout.addView(child, getMyLayoutParams());
	}

	private void addPadding(LinearLayout layout, Context ctx) {
		TextView element = new TextView(ctx);
		element.setHeight(PADDING_VERTICAL);
		element.setBackgroundColor(color.background_dark);
		layout.addView(element, getMyLayoutParams());
	}

	/**
	 * You need to set listener if you want your application to recieve messages
	 * about changing of a current child
	 */
	public void setCurrentChildChangedListener(CustomSpinnerListener listener) {
		this.listener = listener;
		listener.setCustomSpinner(this);
	}

	public static abstract class CustomSpinnerListener extends Handler {

		private CustomSpinner customSpinner;

		abstract public void onScrollChanged(int currentChild);

		final private void setCustomSpinner(CustomSpinner customSpinner) {
			this.customSpinner = customSpinner;
		}

		@Override
		final public void handleMessage(Message msg) {
			onScrollChanged(msg.arg1);
		}

		final private void notifyScrollChanged() {
			this.removeMessages(0);
			final Message msg = Message.obtain();
			msg.arg1 = customSpinner.getCurrentTextViewId();
			sendMessage(msg);
		}
	}
}