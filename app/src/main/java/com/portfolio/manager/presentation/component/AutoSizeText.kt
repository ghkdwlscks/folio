package com.portfolio.manager.presentation.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Single-line text that scales its font to fit the available width.
 *
 * The size is recomputed from [style]'s base font every time the text or the
 * incoming width constraint changes, so it both shrinks to fit AND grows back
 * when space reopens (e.g. leaving split-screen, rotation, or a shorter value).
 * A one-way shrink would stay small forever once narrowed.
 */
@Composable
internal fun AutoSizeText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    minFontSize: TextUnit = 9.sp
) {
    val textMeasurer = rememberTextMeasurer()
    val baseStyle = if (fontWeight != null) style.copy(fontWeight = fontWeight) else style
    val baseFontSize = baseStyle.fontSize.takeIf { it != TextUnit.Unspecified } ?: 14.sp

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val fontSize = remember(text, baseStyle, minFontSize, maxWidthPx) {
            if (maxWidthPx == Int.MAX_VALUE || maxWidthPx <= 0 || text.isEmpty()) {
                baseFontSize
            } else {
                val width = textMeasurer.measure(
                    text = text,
                    style = baseStyle.copy(fontSize = baseFontSize),
                    maxLines = 1,
                    softWrap = false
                ).size.width
                if (width <= maxWidthPx || width <= 0) {
                    baseFontSize
                } else {
                    // Text width scales ~linearly with font size; shrink to fit
                    // with a small margin to absorb sub-pixel rounding.
                    (baseFontSize.value * maxWidthPx / width * 0.98f)
                        .coerceAtLeast(minFontSize.value).sp
                }
            }
        }

        Text(
            text = text,
            style = baseStyle.copy(fontSize = fontSize),
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}
