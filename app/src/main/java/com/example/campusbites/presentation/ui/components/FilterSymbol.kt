package com.example.campusbites.presentation.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object CustomIcons {
    val FilterList: ImageVector
        get() {
            if (_filterList != null) {
                return _filterList!!
            }
            _filterList = ImageVector.Builder(
                name = "FilterList",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 960f,
                viewportHeight = 960f
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1.0f,
                    stroke = null,
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(400f, 720f)
                    verticalLineToRelative(-80f)
                    horizontalLineToRelative(160f)
                    verticalLineToRelative(80f)
                    close()
                    moveTo(240f, 520f)
                    verticalLineToRelative(-80f)
                    horizontalLineToRelative(480f)
                    verticalLineToRelative(80f)
                    close()
                    moveTo(120f, 320f)
                    verticalLineToRelative(-80f)
                    horizontalLineToRelative(720f)
                    verticalLineToRelative(80f)
                    close()
                }
            }.build()
            return _filterList!!
        }

    private var _filterList: ImageVector? = null
}
