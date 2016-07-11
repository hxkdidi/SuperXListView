package com.xlistview;


import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;


/**
 * @author markmjw
 * @date 2013-10-08
 */
public class XFooterView extends LinearLayout {
    public final static int STATE_NORMAL = 0;
    public final static int STATE_READY = 1;
    public final static int STATE_LOADING = 2;

    private final int ROTATE_ANIM_DURATION = 180;

    private View mLayout;

    private View mProgressBar;

    private TextView mHintView;

    // private ImageView mHintImage;

    private Animation mRotateUpAnim;
    private Animation mRotateDownAnim;

    private int mState = STATE_NORMAL;
    private boolean noMore = false;
    private View rela_footer;
    private Animation operatingAnim;
    private boolean isNomoe = false;

    public XFooterView(Context context) {
        super(context);
        initView(context);
    }

    public XFooterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        mLayout = LayoutInflater.from(context)
                .inflate(R.layout.vw_footer, null);
        mLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(mLayout);


        rela_footer = mLayout.findViewById(R.id.rela_footer);
        mProgressBar = mLayout.findViewById(R.id.footer_progressbar);
        mHintView = (TextView) mLayout.findViewById(R.id.footer_hint_text);

        operatingAnim = AnimationUtils.loadAnimation(context, R.anim.image_rotate);
        LinearInterpolator lin = new LinearInterpolator();
        operatingAnim.setInterpolator(lin);

        mProgressBar.startAnimation(operatingAnim);

        // mHintImage = (ImageView) mLayout.findViewById(R.id.footer_arrow);

//		mRotateUpAnim = new RotateAnimation(0.0f, 180.0f,
//				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
//				0.5f);
//		mRotateUpAnim.setDuration(ROTATE_ANIM_DURATION);
//		mRotateUpAnim.setFillAfter(true);
//
//		mRotateDownAnim = new RotateAnimation(180.0f, 0.0f,
//				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
//				0.5f);
//		mRotateDownAnim.setDuration(ROTATE_ANIM_DURATION);
//		mRotateDownAnim.setFillAfter(true);
    }

    /**
     * Set footer view state
     *
     * @param state
     * @see #STATE_LOADING
     * @see #STATE_NORMAL
     * @see #STATE_READY
     */
    public void setState(int state) {
        if (state == mState)
            return;
        if (noMore) {
            return;
        }

        if (state == STATE_LOADING) {
            // mHintImage.clearAnimation();
            // mHintImage.setVisibility(View.INVISIBLE);
            rela_footer.setVisibility(View.VISIBLE);
            mHintView.setVisibility(View.INVISIBLE);
        } else {
            mHintView.setVisibility(View.VISIBLE);
            // mHintImage.setVisibility(View.VISIBLE);
            rela_footer.setVisibility(View.INVISIBLE);
        }

        switch (state) {
            case STATE_NORMAL:
                // if (mState == STATE_READY) {
                // mHintImage.startAnimation(mRotateDownAnim);
                // }
                // if (mState == STATE_LOADING) {
                // mHintImage.clearAnimation();
                // }
                mHintView.setText(R.string.footer_hint_load_normal);
                break;
            case STATE_READY:
                if (mState != STATE_READY) {
                    // mHintImage.clearAnimation();
                    // mHintImage.startAnimation(mRotateUpAnim);
                    mHintView.setText(R.string.footer_hint_load_ready);
                }
                break;
            case STATE_LOADING:


                break;
        }

        mState = state;
    }

    public void setNoMore(boolean noMore) {
        isNomoe = noMore;
//		this.noMore=noMore;
//		rela_footer.setVisibility(View.INVISIBLE);
//		mHintView.setVisibility(View.VISIBLE);
//		mHintView.setText(R.string.footer_hint_no_more);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mLayout
                .getLayoutParams();
        lp.height = 0;
//		lp.height =  LinearLayout.LayoutParams.WRAP_CONTENT;
        mLayout.setLayoutParams(lp);
    }

    /**
     * Set footer view bottom margin.
     *
     * @param margin
     */
    public void setBottomMargin(int margin) {
        if (margin < 0)
            return;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mLayout
                .getLayoutParams();
        lp.bottomMargin = margin;
        mLayout.setLayoutParams(lp);
    }

    /**
     * Get footer view bottom margin.
     *
     * @return
     */
    public int getBottomMargin() {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mLayout
                .getLayoutParams();
        return lp.bottomMargin;
    }

    /**
     * normal status
     */
    public void normal() {
        mHintView.setVisibility(View.VISIBLE);
        rela_footer.setVisibility(View.INVISIBLE);
    }

    /**
     * loading status
     */
    public void loading() {
        mHintView.setVisibility(View.GONE);
        rela_footer.setVisibility(View.VISIBLE);
    }

    /**
     * hide footer when disable pull load more
     */
    public void hide() {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mLayout
                .getLayoutParams();
        if (isNomoe) {
            lp.height = 0;
        } else {
            lp.height = LayoutParams.WRAP_CONTENT;
        }
        mLayout.setLayoutParams(lp);
        mHintView.setText("没有更多了");
        normal();
    }

    /**
     * show footer
     */
    public void show() {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mLayout
                .getLayoutParams();
        lp.height = LayoutParams.WRAP_CONTENT;
        mHintView.setText(R.string.footer_hint_load_normal);
        mLayout.setLayoutParams(lp);
    }

}
