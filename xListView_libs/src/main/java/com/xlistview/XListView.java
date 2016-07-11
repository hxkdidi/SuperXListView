package com.xlistview;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;

import java.util.logging.Logger;

/**
 * XListView, it's based on <a
 * href="https://github.com/Maxwin-z/XListView-Android">XListView(Maxwin)</a>
 * 
 * @author markmjw
 * @date 2013-10-08
 */
public class XListView extends ListView implements OnScrollListener {
	// private static final String TAG = "XListView";

	private final static int SCROLL_BACK_HEADER = 0;
	private final static int SCROLL_BACK_FOOTER = 1;

	private final static int SCROLL_DURATION = 400;

	// when pull up >= 50px
	private final static int PULL_LOAD_MORE_DELTA = 50;

	// support iOS like pull
	private final static float OFFSET_RADIO = 1.8f;

	private float mLastY = -1;

	// used for scroll back
	// private Scroller mScroller;
	private Scroller mTopScroller;
	private Scroller mBtmScroller;
	// user's scroll listener
	private OnScrollListener mScrollListener;
	// for mScroller, scroll back from header or footer.
	private int mScrollBack;

	// the interface to trigger refresh and load more.
	private IXListViewListener mListener;

	private XHeaderView mHeader;
	// header view content, use it to calculate the Header's height. And hide it
	// when disable pull refresh.
	private RelativeLayout mHeaderContent;
	private TextView mHeaderTime;
	private int mHeaderHeight = 150;

	private LinearLayout mFooterLayout;
	private XFooterView mFooterView;
	private boolean mIsFooterReady = false;

	private boolean mEnablePullRefresh = true;
	private boolean mPullRefreshing = false;

	private boolean mEnablePullLoad = true;
	private boolean mEnableAutoLoad = false;
	private boolean mPullLoading = false;

	// total list items, used to detect is at the bottom of ListView
	private int mTotalItemCount;

	private boolean noMore = false;
	private boolean refresh;
	private boolean loadMore;
	private boolean ISMOVE= false;
	private long refreshTime=0;
	private long loadTime=0;

	public XListView(Context context) {
		super(context);
		initWithContext(context);
	}

	public XListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initWithContext(context);
	}

	public XListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initWithContext(context);
	}

	private void initWithContext(Context context) {
		setOverScrollMode(OVER_SCROLL_NEVER);
		mTopScroller = new Scroller(context, new DecelerateInterpolator());
		mBtmScroller = new Scroller(context, new DecelerateInterpolator());
		super.setOnScrollListener(this);

		// init header view
		mHeader = new XHeaderView(context);
		mHeaderContent = (RelativeLayout) mHeader.findViewById(R.id.header_content);
		mHeaderTime = (TextView) mHeader.findViewById(R.id.header_hint_time);
		addHeaderView(mHeader);
       mHeaderHeight = (int) context.getResources().getDimension(R.dimen.header);
		// init footer view
		mFooterView = new XFooterView(context);
		mFooterLayout = new LinearLayout(context);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.CENTER;
		mFooterLayout.addView(mFooterView, params);

		// init header height
		ViewTreeObserver observer = mHeader.getViewTreeObserver();
		if (null != observer) {
			observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@SuppressWarnings("deprecation")
				@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
				@Override
				public void onGlobalLayout() {
					mHeaderHeight = mHeaderContent.getHeight();
					ViewTreeObserver observer = getViewTreeObserver();

					if (null != observer) {
						if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
							observer.removeGlobalOnLayoutListener(this);
						} else {
							observer.removeOnGlobalLayoutListener(this);
						}
					}
				}
			});
		}
	}

	public static int dp2px(Context context, float dpValue) {
		return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.getResources()
				.getDisplayMetrics()) + 0.5);
	}


	@Override
	public void setAdapter(ListAdapter adapter) {
		// make sure XFooterView is the last footer view, and only add once.
		if (!mIsFooterReady) {
			mIsFooterReady = true;
			addFooterView(mFooterLayout);
		}

		super.setAdapter(adapter);
	}

	/**
	 * Enable or disable pull down refresh feature.
	 * 
	 * @param enable
	 */
	public void setPullRefreshEnable(boolean enable) {
		mEnablePullRefresh = enable;

		// disable, hide the content
		mHeaderContent.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
	}

	/**
	 * Enable or disable pull up load more feature.
	 * 
	 * @param enable
	 */
	public void setPullLoadEnable(boolean enable) {
		mEnablePullLoad = enable;
		if (!mEnablePullLoad) {
			mFooterView.setBottomMargin(0);
			// mFooterView.setPadding(0, 0, 0, mFooterView.getHeight() * (-1));
			mFooterView.setPadding(0, 0, 0, 0);
			mFooterView.setOnClickListener(null);
			mFooterView.hide();
		} else {
			mFooterView.setPadding(0, 0, 0, 0);
			mFooterView.setState(XFooterView.STATE_NORMAL);
			// both "pull up" and "click" will invoke load more.
			mFooterView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startLoadMore(true);
				}
			});
//			mFooterView.setVisibility(VISIBLE);
			mFooterView.show();
		}
	}

	/**
	 * Enable or disable auto load more feature when scroll to bottom.
	 * 
	 * @param enable
	 */
	public void setAutoLoadEnable(boolean enable) {
		mEnableAutoLoad = enable;
	}

	/**
	 * Stop refresh, reset header view.
	 */
	public void stopRefresh() {
		final long rtime = System.currentTimeMillis();
		Log.e("refresh","stopRefresh time="+rtime);
		if (rtime-refreshTime<2000){
			postDelayed(new Runnable() {
				@Override
				public void run() {
					if (mPullRefreshing) {
						mPullRefreshing = false;
						resetHeaderHeight();
						refreshTime = rtime;
					}
				}
			},2000+refreshTime-rtime);
		}else {
			if (mPullRefreshing) {
				mPullRefreshing = false;
				resetHeaderHeight();
				refreshTime = rtime;
			}
		}
	}

	/**
	 * Stop load more, reset footer view.
	 */
	public void stopLoadMore() {
//		final long ltime = System.currentTimeMillis();
//		if (ltime-loadTime<2000){
//			postDelayed(new Runnable() {
//				@Override
//				public void run() {
//					if (mPullLoading) {
//						mPullLoading = false;
//						mFooterView.setState(XFooterView.STATE_NORMAL);
//						loadTime = ltime;
//					}
//				}
//			},2000+loadTime-ltime);
//		}else {
//			if (mPullLoading) {
//				mPullLoading = false;
//				mFooterView.setState(XFooterView.STATE_NORMAL);
//				loadTime = ltime;
//			}
//		}
		if (mPullLoading) {
			mPullLoading = false;
			mFooterView.setState(XFooterView.STATE_NORMAL);
		}
	}

	/**
	 * Set last refresh time
	 * 
	 * @param time
	 */
	public void setRefreshTime(String time) {
//		mHeaderTime.setText(time);
	}

	/**
	 * Set listener.
	 * 
	 * @param listener
	 */
	public void setXListViewListener(IXListViewListener listener) {
		mListener = listener;
	}

	/**
	 * ??????????
	 * 
	 * @author mcp
	 */
	public void setNoMore() {
		mFooterView.setNoMore(true);
	}

	/**
	 * Auto call back refresh.
	 */
	public void autoRefresh() {
		Log.e("mHeaderHeight","mHeaderHeight="+mHeaderHeight);
		mHeader.setVisibleHeight(mHeaderHeight);

		if (mEnablePullRefresh && !mPullRefreshing) {
			// update the arrow image not refreshing
			if (mHeader.getVisibleHeight() > mHeaderHeight) {
				mHeader.setState(XHeaderView.STATE_READY);
			} else {
				mHeader.setState(XHeaderView.STATE_NORMAL);
			}
		}

//		mPullRefreshing = true;
		mHeader.setState(XHeaderView.STATE_REFRESHING);
		refresh();
	}

	private void invokeOnScrolling() {
		if (mScrollListener instanceof OnXScrollListener) {
			OnXScrollListener listener = (OnXScrollListener) mScrollListener;
			listener.onXScrolling(this);
		}
	}

	public void updateHeaderHeight(float delta) {
		mHeader.setVisibleHeight((int) delta + mHeader.getVisibleHeight());

		if (mEnablePullRefresh && !mPullRefreshing) {
			// update the arrow image unrefreshing
			if (mHeader.getVisibleHeight() > mHeaderHeight) {
				mHeader.setState(XHeaderView.STATE_READY);
			} else {
				mHeader.setState(XHeaderView.STATE_NORMAL);
			}
		}

		// scroll to top each time
		setSelection(0);
	}

	private void resetHeaderHeight() {
		int height = mHeader.getVisibleHeight();
		if (height == 0)
			return;

		// refreshing and header isn't shown fully. do nothing.
		if (mPullRefreshing && height <= mHeaderHeight)
			return;

		// default: scroll back to dismiss header.
		int finalHeight = 0;
		// is refreshing, just scroll back to show all the header.
		if (mPullRefreshing && height > mHeaderHeight) {
			finalHeight = mHeaderHeight;
		}

		mScrollBack = SCROLL_BACK_HEADER;
		mTopScroller.startScroll(0, height, 0, finalHeight - height, SCROLL_DURATION);

		// trigger computeScroll
		postDelayed(new Runnable() {
			@Override
			public void run() {
				ISMOVE = false;
			}
		},SCROLL_DURATION);
		invalidate();
	}

	private void updateFooterHeight(float delta) {
		if (!mEnablePullLoad){
			return;
		}

		int height = mFooterView.getBottomMargin() + (int) delta;
//		if (mEnablePullLoad && !mPullLoading) {
//			if (height > PULL_LOAD_MORE_DELTA) {
//				// height enough to invoke load more.
//				mFooterView.setState(XFooterView.STATE_READY);
//			} else {
//				mFooterView.setState(XFooterView.STATE_NORMAL);
//			}
//		}
		// Log.d("xListView", "updateFooterHeight:"+delta+";newHeight:"+height);
		mFooterView.setBottomMargin(height);

		// scroll to bottom
		// setSelection(mTotalItemCount - 1);
	}

	private int resetFooterHeight() {
		final int bottomMargin = mFooterView.getBottomMargin();
		if (bottomMargin > 0) {
			mScrollBack = SCROLL_BACK_FOOTER;
			mBtmScroller.startScroll(0, bottomMargin, 0, -bottomMargin, SCROLL_DURATION);
			invalidate();
		}
		postDelayed(new Runnable() {
			@Override
			public void run() {
				ISMOVE = false;
				if (bottomMargin > 300 && mEnablePullLoad) {
					startLoadMore(true);
				}
			}
		}, SCROLL_DURATION);

		return bottomMargin;
	}

	public void startLoadMore(boolean isLoadMore) {
		if (noMore||mPullLoading)
			return;
		mFooterView.setState(XFooterView.STATE_NORMAL);
		if (isLoadMore){
			loadMore();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mLastY == -1) {
			mLastY = ev.getRawY();
		}

		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			ISMOVE = false;
			mLastY = ev.getRawY();
			break;

		case MotionEvent.ACTION_MOVE:
			ISMOVE = true;
			final float deltaY = ev.getRawY() - mLastY;
			mLastY = ev.getRawY();

			if (getFirstVisiblePosition() == 0 && (mHeader.getVisibleHeight() > 0 || deltaY > 0)) {
				// the first item is showing, header has shown or pull down.
				updateHeaderHeight(deltaY / OFFSET_RADIO);
				invokeOnScrolling();

			}
			if (getLastVisiblePosition() == mTotalItemCount - 1 && (mFooterView.getBottomMargin() > 0 || deltaY < 0)) {
				// last item, already pulled up or want to pull up.
				updateFooterHeight(-deltaY / OFFSET_RADIO);
			}
			break;

		default:
			// reset
			ISMOVE = true;
			mLastY = -1;
			if (getFirstVisiblePosition() == 0) {
				// invoke refresh
				if (mEnablePullRefresh && mHeader.getVisibleHeight() > mHeaderHeight&&!mPullRefreshing) {
//					mPullRefreshing = true;
					mHeader.setState(XHeaderView.STATE_REFRESHING);
					refresh();
				}
				resetHeaderHeight();
			}
			if (getLastVisiblePosition() == mTotalItemCount - 1) {
				// invoke load more.
				if (mEnablePullLoad && mFooterView.getBottomMargin() > PULL_LOAD_MORE_DELTA) {
					startLoadMore(false);
				}
				resetFooterHeight();
			}
			break;
		}
		return super.onTouchEvent(ev);
	}

	@Override
	public void computeScroll() {
		// if (mScroller.computeScrollOffset()) {
		// if (mScrollBack == SCROLL_BACK_HEADER) {
		// mHeader.setVisibleHeight(mScroller.getCurrY());
		// } else {
		// mFooterView.setBottomMargin(mScroller.getCurrY());
		// }
		// postInvalidate();
		// invokeOnScrolling();
		// }
		boolean scroll=false;
		if (mTopScroller.computeScrollOffset()) {
			mHeader.setVisibleHeight(mTopScroller.getCurrY());
			scroll=true;
		}
		if (mBtmScroller.computeScrollOffset()) {
			mFooterView.setBottomMargin(mBtmScroller.getCurrY());
			scroll=true;
		}
		if(scroll){
			postInvalidate();
			invokeOnScrolling();
		}
		super.computeScroll();
	}

	@Override
	public void setOnScrollListener(OnScrollListener l) {
		mScrollListener = l;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (mScrollListener != null) {
			mScrollListener.onScrollStateChanged(view, scrollState);
		}

		if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
			if (mEnableAutoLoad && getLastVisiblePosition() == getCount() - 1) {
				startLoadMore(false);
			}
		}
		Log.e("scrollState", "y=" + view.getScaleY());
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// send to user's listener
		mTotalItemCount = totalItemCount;
		if (mScrollListener != null) {
			mScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
		}
	}

	private void refresh() {
		if (mEnablePullRefresh && null != mListener&&!mPullRefreshing&&!mPullLoading) {
			mPullRefreshing = true;
			mListener.onRefresh();
			refreshTime = System.currentTimeMillis();
			Log.e("refresh","refreshTime="+refreshTime);
		}
	}

	private void loadMore() {
		if (mEnablePullLoad && null != mListener&&!mPullLoading&&!mPullRefreshing) {
			mPullLoading = true;
			mFooterView.setState(XFooterView.STATE_LOADING);
			mListener.onLoadMore();
			loadTime = System.currentTimeMillis();
			Log.e("refresh","loadTime="+loadTime);
		}
	}

	/**
	 * 是否在刷新
	 * @return
	 */
	public boolean isRefresh() {
		return mPullRefreshing;
	}

	/***
	 * 是否在加载更多
	 * @return
	 */
	public boolean isLoadMore() {
		return mPullLoading;
	}

	public boolean getIsMove() {
		return ISMOVE;
	}

	/**
	 * You can listen ListView.OnScrollListener or this one. it will invoke
	 * onXScrolling when header/footer scroll back.
	 */
	public interface OnXScrollListener extends OnScrollListener {
		public void onXScrolling(View view);
	}

	/**
	 * Implements this interface to get refresh/load more event.
	 * 
	 * @author markmjw
	 */
	public interface IXListViewListener {
		public void onRefresh();

		public void onLoadMore();
	}

}
