package com.mercadopago.android.px.format

import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.mercadopago.android.px.BasicRobolectricTest
import com.mercadopago.android.px.R
import com.mercadopago.android.px.internal.font.PxFont
import com.mercadopago.android.px.internal.util.JsonUtil
import com.mercadopago.android.px.internal.util.ViewUtils
import com.mercadopago.android.px.internal.util.textformatter.InterestFormatter
import com.mercadopago.android.px.model.internal.Text
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InterestRateFormatterTest : BasicRobolectricTest() {

    private lateinit var expected: SpannableStringBuilder
    private lateinit var actual: SpannableStringBuilder

    @Before
    fun setUp() {
        expected = SpannableStringBuilder()
        actual = SpannableStringBuilder()
    }

    @Test
    fun whenInterestRateIsCFT() {
        setFontAndColor(expected, "3x \$47,88", PxFont.SEMI_BOLD, ContextCompat.getColor(getContext(), R.color.ui_meli_black))
        setFontAndColor(expected, "(\$143,64)", PxFont.REGULAR, ContextCompat.getColor(getContext(), R.color.ui_meli_grey))
        setFontAndColor(expected, "CFT: 199,26%", PxFont.REGULAR, ContextCompat.getColor(getContext(), R.color.ui_meli_grey))

        val cftText = JsonUtil
                .fromJson("{\"message\":\"CFT: 199,26%\",\"text_color\":\"#999999\",\"weight\":\"regular\"}",
                        Text::class.java)

        setFontAndColor(actual, "3x \$47,88", PxFont.SEMI_BOLD, ContextCompat.getColor(getContext(), R.color.ui_meli_black))
        setFontAndColor(actual, "(\$143,64)", PxFont.REGULAR, ContextCompat.getColor(getContext(), R.color.ui_meli_grey))
        InterestFormatter(actual).withText(cftText!!).apply(getContext())

        assertEqualsContent()
    }

    @Test
    fun whenInterestRateIsCFTNA() {

        setFontAndColor(expected, "3x \$45,88", PxFont.SEMI_BOLD, ContextCompat.getColor(getContext(), R.color.ui_meli_black))
        setFontAndColor(expected, "(\$137,64)", PxFont.REGULAR, ContextCompat.getColor(getContext(), R.color.ui_meli_grey))
        setFontAndColor(expected, "CFTNA: 96,8%", PxFont.REGULAR, ContextCompat.getColor(getContext(), R.color.ui_meli_grey))

        val cftnaText = JsonUtil
                .fromJson("{\"message\":\"CFTNA: 96,8%\",\"text_color\":\"#999999\",\"weight\":\"regular\"}",
                        Text::class.java)

        setFontAndColor(actual, "3x \$45,88", PxFont.SEMI_BOLD, ContextCompat.getColor(getContext(), R.color.ui_meli_black))
        setFontAndColor(actual, "(\$137,64)", PxFont.REGULAR, ContextCompat.getColor(getContext(), R.color.ui_meli_grey))
        InterestFormatter(actual).withText(cftnaText!!).apply(getContext())

        assertEqualsContent()
    }

    @Test
    fun whenInterestRateIsCFTEA() {
        setFontAndColor(expected, "3x \$40", PxFont.SEMI_BOLD, ContextCompat.getColor(getContext(), R.color.ui_meli_black))
        setFontAndColor(expected, "Interest-free", PxFont.REGULAR, ContextCompat.getColor(getContext(), R.color.ui_components_success_color))
        setFontAndColor(expected, "CFTEA: 0,00%", PxFont.REGULAR, ContextCompat.getColor(getContext(), R.color.ui_meli_grey))

        val cfteaText = JsonUtil
                .fromJson("{\"message\":\"CFTEA: 0,00%\",\"text_color\":\"#999999\",\"weight\":\"regular\"}",
                        Text::class.java)

        setFontAndColor(actual, "3x \$40", PxFont.SEMI_BOLD, ContextCompat.getColor(getContext(), R.color.ui_meli_black))
        setFontAndColor(actual, "Interest-free", PxFont.REGULAR, ContextCompat.getColor(getContext(), R.color.ui_components_success_color))
        InterestFormatter(actual).withText(cfteaText!!).apply(getContext())

        assertEqualsContent()
    }

    @Test
    fun whenInterestRateTextMessageIsEmpty() {
        setFontAndColor(expected, "3x \$47,88", PxFont.SEMI_BOLD, ContextCompat.getColor(getContext(), R.color.ui_meli_black))
        setFontAndColor(expected, "(\$143,64)", PxFont.REGULAR, ContextCompat.getColor(getContext(), R.color.ui_meli_grey))

        val cftText = JsonUtil
                .fromJson("{\"text_color\":\"#999999\",\"weight\":\"regular\"}",
                        Text::class.java)

        setFontAndColor(actual, "3x \$47,88", PxFont.SEMI_BOLD, ContextCompat.getColor(getContext(), R.color.ui_meli_black))
        setFontAndColor(actual, "(\$143,64)", PxFont.REGULAR, ContextCompat.getColor(getContext(), R.color.ui_meli_grey))
        InterestFormatter(actual).withText(cftText!!).apply(getContext())
        assertEqualsContent()
    }

    private fun setFontAndColor(spannableStringBuilder: SpannableStringBuilder, text: String, font: PxFont, color: Int) {
        val startIndex = spannableStringBuilder.length
        spannableStringBuilder.append(" ").append(text)
        val endIndex = spannableStringBuilder.length

        ViewUtils.setColorInSpannable(color, startIndex, endIndex, spannableStringBuilder)
        ViewUtils.setFontInSpannable(getContext(), font, spannableStringBuilder, startIndex, endIndex)
    }

    private fun assertEqualsContent() {
        val foregroundColorSpan1 = expected.getSpans(0, expected.length, ForegroundColorSpan::class.java)
        val foregroundColorSpan2 = actual.getSpans(0, actual.length, ForegroundColorSpan::class.java)

        assertEquals(foregroundColorSpan1.size, foregroundColorSpan2.size)

        for (i in foregroundColorSpan1.indices) {
            assertEquals(foregroundColorSpan1[i].foregroundColor, foregroundColorSpan2[i].foregroundColor)
        }

        assertEquals(expected.toString(), actual.toString())
    }
}
