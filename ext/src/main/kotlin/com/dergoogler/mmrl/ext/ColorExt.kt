package com.dergoogler.mmrl.ext

import androidx.compose.ui.graphics.Color

fun Color.toCssValue(): String {
    return "#${red.toHex()}${green.toHex()}${blue.toHex()}${alpha.toHex()}"
}
