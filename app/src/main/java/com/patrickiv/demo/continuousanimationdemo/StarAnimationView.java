package com.patrickiv.demo.continuousanimationdemo;

import android.animation.TimeAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/**
 * Continuous animation where stars slide from the bottom to the top
 * Created by Patrick Ivarsson on 7/23/17.
 */
public class StarAnimationView extends View {

    /**
     * Class representing the state of a star
     */
    private static class Star {
        private float x;
        private float y;
        private float scale;
        private float alpha;
        private float speed;
    }

    private static final int BASE_SPEED_DP_PER_S = 200;
    private static final int COUNT = 32;
    private static final int SEED = 1337;

    /** The minimum scale of a star */
    private static final float SCALE_MIN_PART = 0.45f;
    /** How much of the scale that's based on randomness */
    private static final float SCALE_RANDOM_PART = 0.55f;
    /** How much of the alpha that's based on the scale of the star */
    private static final float ALPHA_SCALE_PART = 0.5f;
    /** How much of the alpha that's based on randomness */
    private static final float ALPHA_RANDOM_PART = 0.5f;

    private final Star[] mStars = new Star[COUNT];
    private final Random mRnd = new Random(SEED);

    private TimeAnimator mTimeAnimator;
    private Drawable mDrawable;

    private float mBaseSpeed;
    private float mBaseSize;
    private long mCurrentPlayTime;

    /** @see View#View(Context) */
    public StarAnimationView(Context context) {
        super(context);
        init();
    }

    /** @see View#View(Context, AttributeSet) */
    public StarAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /** @see View#View(Context, AttributeSet, int) */
    public StarAnimationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mDrawable = ContextCompat.getDrawable(getContext(), R.drawable.star);
        mBaseSize = Math.max(mDrawable.getIntrinsicWidth(), mDrawable.getIntrinsicHeight()) / 2f;
        mBaseSpeed = BASE_SPEED_DP_PER_S * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        super.onSizeChanged(width, height, oldw, oldh);

        // The starting position is dependent on the size of the view,
        // which is why the model is initialized here, when the view is measured.
        for (int i = 0; i < mStars.length; i++) {
            final Star star = new Star();
            initializeStar(star, width, height);
            mStars[i] = star;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int viewHeight = getHeight();
        for (final Star star : mStars) {
            // Ignore the star if it's outside of the view bounds
            final float starSize = star.scale * mBaseSize;
            if (star.y + starSize < 0 || star.y - starSize > viewHeight) {
                continue;
            }

            // Save the current canvas state
            final int save = canvas.save();

            // Move the canvas to the center of the star
            canvas.translate(star.x, star.y);

            // Rotate the canvas based on how far the star has moved
            final float progress = (star.y + starSize) / viewHeight;
            canvas.rotate(360 * progress);

            // Prepare the size and alpha of the drawable
            final int size = Math.round(starSize);
            mDrawable.setBounds(-size, -size, size, size);
            mDrawable.setAlpha(Math.round(255 * star.alpha));

            // Draw the star to the canvas
            mDrawable.draw(canvas);

            // Restore the canvas to it's previous position and rotation
            canvas.restoreToCount(save);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mTimeAnimator = new TimeAnimator();
        mTimeAnimator.setTimeListener(new TimeAnimator.TimeListener() {
            @Override
            public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
                if (!isLaidOut()) {
                    // Ignore all calls before the view has been measured and laid out.
                    return;
                }
                updateState(deltaTime);
                invalidate();
            }
        });
        mTimeAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mTimeAnimator.cancel();
        mTimeAnimator.setTimeListener(null);
        mTimeAnimator.removeAllListeners();
        mTimeAnimator = null;
    }

    /**
     * Pause the animation if it's running
     */
    public void pause() {
        if (mTimeAnimator != null && mTimeAnimator.isRunning()) {
            // Store the current play time for later.
            mCurrentPlayTime = mTimeAnimator.getCurrentPlayTime();
            mTimeAnimator.pause();
        }
    }

    /**
     * Resume the animation if not already running
     */
    public void resume() {
        if (mTimeAnimator != null && mTimeAnimator.isPaused()) {
            mTimeAnimator.start();
            // Why set the current play time?
            // TimeAnimator uses timestamps internally to determine the delta given
            // in the TimeListener. When resumed, the next delta received will the whole
            // pause duration, which might cause a huge jank in the animation.
            // By setting the current play time, it will pick of where it left off.
            mTimeAnimator.setCurrentPlayTime(mCurrentPlayTime);
        }
    }

    /**
     * Progress the animation by moving the stars based on the elapsed time
     * @param deltaMs time delta since the last frame, in millis
     */
    private void updateState(float deltaMs) {
        // Converting to seconds since PX/S constants are easier to understand
        final float deltaSeconds = deltaMs / 1000f;
        final int viewWidth = getWidth();
        final int viewHeight = getHeight();

        for (final Star star : mStars) {
            // Move the star based on the elapsed time and it's speed
            star.y -= star.speed * deltaSeconds;

            // If the star is completely outside of the view bounds after
            // updating it's position, recycle it.
            final float size = star.scale * mBaseSize;
            if (star.y + size < 0) {
                initializeStar(star, viewWidth, viewHeight);
            }
        }
    }

    /**
     * Initialize the given star by randomizing it's position, scale and alpha
     * @param star the star to initialize
     * @param viewWidth the view width
     * @param viewHeight the view height
     */
    private void initializeStar(Star star, int viewWidth, int viewHeight) {
        // Set the scale based on a min value and a random multiplier
        star.scale = SCALE_MIN_PART + SCALE_RANDOM_PART * mRnd.nextFloat();

        // Set X to a random value within the width of the view
        star.x = viewWidth * mRnd.nextFloat();

        // Set the Y position
        // Start at the bottom of the view
        star.y = viewHeight;
        // The Y value is in the center of the star, add the size
        // to make sure it starts outside of the view bound
        star.y += star.scale * mBaseSize;
        // Add a random offset to create a small delay before the
        // star appears again.
        star.y += viewHeight * mRnd.nextFloat() / 4f;

        // The alpha is determined by the scale of the star and a random multiplier.
        star.alpha = ALPHA_SCALE_PART * star.scale + ALPHA_RANDOM_PART * mRnd.nextFloat();
        // The bigger and brighter a star is, the faster it moves
        star.speed = mBaseSpeed * star.alpha * star.scale;
    }
}

