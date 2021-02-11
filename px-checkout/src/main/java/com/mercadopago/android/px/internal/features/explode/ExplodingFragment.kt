package com.mercadopago.android.px.internal.features.explode

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mercadopago.android.px.R
import com.mercadopago.android.px.internal.extensions.gone
import com.mercadopago.android.px.internal.extensions.invisible
import com.mercadopago.android.px.internal.extensions.isNotNullNorEmpty
import com.mercadopago.android.px.internal.extensions.visible
import com.mercadopago.android.px.internal.util.ViewUtils
import kotlin.math.hypot

class ExplodingFragment : Fragment() {

    private var animator: ObjectAnimator? = null

    private lateinit var progressBar: ProgressBar
    private lateinit var icon: ImageView
    private lateinit var circle: ImageView
    private lateinit var reveal: View
    private lateinit var text: TextView
    private lateinit var rootView: ViewGroup
    private lateinit var loadingContainer: View

    private var explodeDecorator: ExplodeDecorator? = null
    private var buttonHeight = 0
    private var buttonYPosition = 0
    private var buttonText: CharSequence? = null
    private var maxLoadingTime = 0

    private var handler: Handler? = null
    private val layoutChangeListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        handler?.let {
            updateValuesFromParentView(it.getParentView())
        }
    }
    private val activityContentView: ViewGroup?
        get() = activity?.findViewById(android.R.id.content)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            buttonText = it.getCharSequence(ARG_PROGRESS_TEXT)
            maxLoadingTime = it.getInt(ARG_TIMEOUT)
        } ?: error("Missing explode params")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        explodeDecorator = savedInstanceState?.getParcelable(BUNDLE_DECORATOR)
        rootView = (inflater.inflate(R.layout.px_fragment_exploding, activityContentView) as ViewGroup).also {
            circle = it.findViewById(R.id.cho_loading_buy_circular)
            icon = it.findViewById(R.id.cho_loading_buy_icon)
            reveal = it.findViewById(R.id.cho_loading_buy_reveal)
            text = it.findViewById<TextView>(R.id.cho_loading_buy_progress_text).apply {
                if (buttonText.isNotNullNorEmpty()) {
                    text = buttonText
                }
            }
            loadingContainer = it.findViewById(R.id.cho_loading_buy_container)
            progressBar = it.findViewById<ProgressBar>(R.id.cho_loading_buy_progress).apply {
                max = maxLoadingTime
            }
        }
        return null
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    override fun onAttach(context: Context) {
        super.onAttach(context)
        handler = when {
            context is Handler -> context
            targetFragment is Handler -> targetFragment
            parentFragment is Handler -> parentFragment
            else -> error("ExplodingFragment needs a handler")
        } as Handler
    }

    override fun onDetach() {
        super.onDetach()
        handler = null
    }

    override fun onStart() {
        super.onStart()
        rootView.addOnLayoutChangeListener(layoutChangeListener)
        handler?.let {
            updateValuesFromParentView(it.getParentView())
        }

        // start loading assuming the worst time possible
        animator = ObjectAnimator.ofInt(progressBar, "progress", 0, maxLoadingTime).apply {
            interpolator = LinearInterpolator()
            duration = maxLoadingTime.toLong()
            start()
        }
        explodeDecorator?.let {
            finishLoading(it)
        }
    }

    override fun onStop() {
        super.onStop()
        rootView.removeOnLayoutChangeListener(layoutChangeListener)
    }

    override fun onDestroyView() {
        activityContentView?.apply {
            removeViewAt(childCount - 1)
        }
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(BUNDLE_DECORATOR, explodeDecorator)
        super.onSaveInstanceState(outState)
    }

    fun hasFinished() = explodeDecorator == null

    /**
     * Notify this view that the loading has finish so as to start the finish anim.
     *
     * @param explodeDecorator information about the order result, useful for styling the view.
     */
    fun finishLoading(explodeDecorator: ExplodeDecorator) {
        this.explodeDecorator = explodeDecorator
        doFinishLoading()
    }

    private fun adjustHeight(view: ImageView) {
        val params = view.layoutParams
        params.height = buttonHeight
        params.width = buttonHeight
        view.layoutParams = params
    }

    /**
     * Transform the progress bar into the result icon background. The color and the shape are animated.
     */
    private fun createResultAnim(explodeDecorator: ExplodeDecorator) {
        context?.let { context ->
            @ColorInt val color = explodeDecorator.getDarkPrimaryColor(context)
            circle.setColorFilter(color)
            icon.setImageResource(explodeDecorator.statusIcon)
            val duration = resources.getInteger(R.integer.px_long_animation_time)
            val initialWidth = progressBar.width
            val finalSize = progressBar.height
            val initialRadius = resources.getDimensionPixelOffset(R.dimen.px_xxxs_margin)
            val finalRadius = finalSize / 2
            val initialBg = getProgressBarShape(ContextCompat.getColor(context, R.color.ui_action_button_pressed), initialRadius)
            val finalBg = getProgressBarShape(color, initialRadius)
            val transitionDrawable = TransitionDrawable(arrayOf<Drawable>(initialBg, finalBg))
            progressBar.progressDrawable = transitionDrawable
            transitionDrawable.startTransition(duration)
            ValueAnimator.ofFloat(0f, 1f).apply {
                addUpdateListener(object : AnimatorUpdateListener {
                    override fun onAnimationUpdate(animation: ValueAnimator) {
                        val interpolatedTime = animation.animatedFraction
                        val radius = getNewRadius(interpolatedTime)
                        setRadius(initialBg, radius)
                        setRadius(finalBg, radius)
                        progressBar.layoutParams.width = getNewWidth(interpolatedTime)
                        progressBar.requestLayout()
                    }

                    private fun getNewRadius(t: Float): Int {
                        return initialRadius + ((finalRadius - initialRadius) * t).toInt()
                    }

                    private fun getNewWidth(t: Float): Int {
                        return initialWidth + ((finalSize - initialWidth) * t).toInt()
                    }

                    private fun setRadius(bg: Drawable, value: Int) {
                        val layerBg = bg as GradientDrawable
                        layerBg.cornerRadius = value.toFloat()
                    }
                })
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        removeAllListeners()
                        removeAllUpdateListeners()
                        createResultIconAnim()
                    }
                })
                interpolator = DecelerateInterpolator(2f)
                this.duration = duration.toLong()
                start()
            }
            text.gone()
        }
    }

    /**
     * @return the shape of the progress bar to transform
     */
    private fun getProgressBarShape(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }
    }

    /**
     * Now that the icon background is visible, animate the icon. The icon will start big and transparent and become
     * small and opaque
     */
    private fun createResultIconAnim() {
        if (isAdded) {
            progressBar.invisible()
            icon.visible()
            circle.visible()
            icon.scaleY = ICON_SCALE
            icon.scaleX = ICON_SCALE
            icon.alpha = 0f
            icon.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f)
                .setInterpolator(DecelerateInterpolator(2f))
                .setDuration(resources.getInteger(R.integer.px_default_animation_time).toLong())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        handler?.onResultIconAnimation()
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        animation.removeAllListeners()
                        explodeDecorator?.let {
                            createCircularReveal(it)
                        }
                    }
                }).start()
        }
    }

    /**
     * Wait so that the icon is visible for a while.. then fill the whole screen with the appropriate color.
     */
    private fun createCircularReveal(explodeDecorator: ExplodeDecorator) {
        context?.let { context ->
            // when the icon anim has finished, paint the whole screen with the result color
            val finalRadius = hypot(rootView.width.toDouble(), rootView.height.toDouble()).toFloat()
            val startRadius = buttonHeight / 2
            val cx = (progressBar.left + progressBar.right) / 2
            val cy = (progressBar.top + progressBar.bottom) / 2 + buttonYPosition
            val startColor = explodeDecorator.getDarkPrimaryColor(context)
            val endColor = explodeDecorator.getPrimaryColor(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ViewAnimationUtils.createCircularReveal(reveal, cx, cy, startRadius.toFloat(), finalRadius)
            } else {
                ObjectAnimator.ofFloat(reveal, "alpha", 0f, 1f)
            }.apply {
                duration = resources.getInteger(R.integer.px_long_animation_time).toLong()
                startDelay = resources.getInteger(R.integer.px_long_animation_time).toLong()
                interpolator = AccelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        circle.gone()
                        icon.gone()
                        reveal.visible()
                        val switchColors = arrayOf<Drawable>(ColorDrawable(startColor), ColorDrawable(endColor))
                        val colorSwitch = TransitionDrawable(switchColors)
                        reveal.background = colorSwitch
                        colorSwitch.startTransition(animation.duration.toInt())
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        onCircularRevealEnd(endColor)
                    }
                })
                start()
            }
        }
    }

    private fun tintStatusBar(color: Int) {
        activity?.let {
            ViewUtils.setStatusBarColor(color, it.window)
        }
    }

    private fun doFinishLoading() {
        // now finish the remaining loading progress
        val progress = progressBar.progress
        animator?.cancel()
        animator = ObjectAnimator.ofInt(progressBar, "progress", progress, maxLoadingTime).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = resources.getInteger(R.integer.px_long_animation_time).toLong()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    removeListener(this)
                    explodeDecorator?.let {
                        createResultAnim(it)
                    }
                }
            })
            start()
        }
    }

    private fun onCircularRevealEnd(endColor: Int) {
        explodeDecorator = null
        tintStatusBar(endColor)
        handler?.onAnimationFinished()
    }

    private fun updateValuesFromParentView(parentView: View) {
        activityContentView?.let {
            val activityLocation = IntArray(2)
            it.getLocationOnScreen(activityLocation)
            val parentLocation = IntArray(2)
            parentView.getLocationOnScreen(parentLocation)
            buttonYPosition = parentLocation[1] - activityLocation[1]
            buttonHeight = parentView.height
            val progressBarParams = progressBar.layoutParams as MarginLayoutParams
            progressBarParams.height = buttonHeight
            progressBarParams.leftMargin = parentLocation[0]
            progressBarParams.rightMargin = parentLocation[0]
            progressBar.setPadding(parentView.paddingStart, parentView.paddingTop, parentView.paddingRight,
                parentView.paddingEnd)
            progressBar.layoutParams = progressBarParams
            adjustHeight(circle)
            adjustHeight(icon)
            loadingContainer.y = buttonYPosition.toFloat()
        }
    }

    companion object {
        const val TAG = "TAG_EXPLODING_FRAGMENT"
        private const val BUNDLE_DECORATOR = "BUNDLE_DECORATOR"
        private const val ARG_PROGRESS_TEXT = "ARG_PROGRESS_TEXT"
        private const val ARG_TIMEOUT = "ARG_TIMEOUT"
        const val ICON_SCALE = 3.0f
        fun newInstance(progressText: CharSequence, timeout: Int): ExplodingFragment {
            return ExplodingFragment().apply {
                arguments = Bundle().apply {
                    putCharSequence(ARG_PROGRESS_TEXT, progressText)
                    putInt(ARG_TIMEOUT, timeout)
                }
            }
        }
    }

    interface Handler {
        fun getParentView(): View
        fun onResultIconAnimation()
        fun onAnimationFinished()
    }
}
