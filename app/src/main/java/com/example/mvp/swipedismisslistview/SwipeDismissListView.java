package com.example.mvp.swipedismisslistview;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;

public class SwipeDismissListView extends ListView {

    public static final String TAG = "SwipeDismissListView";

    //认为用户滑动的最小距离
    private int mSlop;
    //滑动的最小速度
    private int mMinFlingVelocity;
    //滑动的最大距离
    private int mMaxFlingVelocity;

    //手指按下时的坐标信息
    private View mDownView;
    private float mDownX;
    private float mDownY;
    //手指按下时的List的item
    private int mDownPosition;
    //点击的item的宽度
    private int mViewWidth;
    //滑动速度检测类
    private VelocityTracker mVelocityTracker;
    //用于标识是否正在进行滑动
    private boolean mSwiping;

    //执行动画的时间
    private int mAnimtionTime = 150;

    private OnDismissCallBack onDismissCallBack;

    public void setOnDismissCallBack(OnDismissCallBack onDismissCallBack) {
        this.onDismissCallBack = onDismissCallBack;
    }

    public SwipeDismissListView(Context context) {
        this(context, null);
    }

    public SwipeDismissListView(Context context, AttributeSet attrs) {
        this(context, attrs, 1);
    }

    public SwipeDismissListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        ViewConfiguration vc = ViewConfiguration.get(context);
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleDown(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                return handleMove(ev);
            case MotionEvent.ACTION_UP:
                handleUp(ev);
                break;
        }
        return super.onTouchEvent(ev);
    }

    /**
     * 按下的初始位置的记录，以及对象的初始化操作
     *
     * @param ev
     */
    private void handleDown(MotionEvent ev) {
        mDownX = ev.getX();
        mDownY = ev.getY();

        mDownPosition = pointToPosition((int) mDownX, (int) mDownY);
        //当按下的位置在ListView中无效的时候直接结束
        if (mDownPosition == AdapterView.INVALID_POSITION) {
            return;
        }

        mDownView = getChildAt(mDownPosition - getFirstVisiblePosition());

        if (null != mDownView) {
            mViewWidth = mDownView.getWidth();
        }
        //进行滑动速度检测
        mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(ev);
    }

    private boolean handleMove(MotionEvent ev) {
        if (null == mVelocityTracker || mDownView == null) {
            return super.onTouchEvent(ev);
        }
        //获取手指滑动的距离
        float deltaX = ev.getX() - mDownX;
        float deltaY = ev.getY() - mDownY;

        //当水平方向滑动的距离大于最小滑动距离且竖直方向滑动的距离小于最小滑动距离则表示可以进行滑动
        if (Math.abs(deltaX) > mSlop && Math.abs(deltaY) < mSlop) {
            mSwiping = true;
            //当手指正在滑动的时候取消item的点击事件
            MotionEvent cancelEvent = MotionEvent.obtain(ev);//用于将ev重新复制一份
            cancelEvent.setAction(MotionEvent.ACTION_CANCEL | (ev.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
            onTouchEvent(cancelEvent);
        }
        if (mSwiping) {
            ViewHelper.setTranslationX(mDownView, deltaX);
            ViewHelper.setAlpha(mDownView, Math.max(0f, Math.min(1f, 1f - 2f * Math.abs(deltaX) / mViewWidth)));

            return true;
        }
        return super.onTouchEvent(ev);
    }

    private void handleUp(MotionEvent ev) {
        if (null == mVelocityTracker || mDownView == null) {
            return;
        }
        float deltaX = ev.getX() - mDownX;
        //通过滑动的距离计算出x，y方向的滑动速度
        mVelocityTracker.computeCurrentVelocity(1000);
        float velocityX = mVelocityTracker.getXVelocity();
        float velocityY = mVelocityTracker.getYVelocity();

        boolean dismiss = false;//item是否滑出屏幕
        boolean dismissRight = false;//item是否向右滑动

        if (Math.abs(deltaX) > mViewWidth / 2) {
            dismiss = true;
            dismissRight = deltaX > 0;
        } else if (mMinFlingVelocity * 10 < velocityX && mMaxFlingVelocity > velocityX && velocityY < velocityX) {
            dismiss = true;
            dismissRight = velocityX > 0;
        }

        if (dismiss) {
            ViewPropertyAnimator.animate(mDownView)
                    .translationX(dismissRight ? mViewWidth : -mViewWidth)
                    .alpha(0)
                    .setDuration(mAnimtionTime)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            performDismiss(mDownView, mDownPosition);
                        }
                    });
        } else {
            //将动画滑动到初始位置
            ViewPropertyAnimator.animate(mDownView)
                    .translationX(0)
                    .alpha(1)
                    .setDuration(mAnimtionTime)
                    .setListener(null);
        }
        if (null != mVelocityTracker) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
        mSwiping = false;
    }

    //动画结束后执行删除以及下面的item向上挤压
    private void performDismiss(final View dismissView, final int dismissPosition) {
        final ViewGroup.LayoutParams lp = dismissView.getLayoutParams();
        final int originaHeight = dismissView.getHeight();

        final ValueAnimator valueAnimator = ValueAnimator.ofInt(originaHeight, 0).setDuration(mAnimtionTime);
        valueAnimator.start();
        valueAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (null != onDismissCallBack) {
                    onDismissCallBack.onDismiss(dismissPosition);
                }
                //动画结束后要将item设置回来
                ViewHelper.setAlpha(dismissView, 1f);
                ViewHelper.setTranslationX(dismissView, 0);
                ViewGroup.LayoutParams lp1 = dismissView.getLayoutParams();
                lp1.height = originaHeight;
                dismissView.setLayoutParams(lp1);

                dismissView.clearAnimation();
                Log.i(TAG, "onAnimationEnd: " + dismissView.getAnimation());
            }
        });
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                //当前item下面的item向上进行移动
                lp.height = (int) valueAnimator.getAnimatedValue();
                dismissView.setLayoutParams(lp);
            }
        });
    }

}
