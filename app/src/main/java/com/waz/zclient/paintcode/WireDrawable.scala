/**
 * Wire
 * Copyright (C) 2018 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.paintcode

import android.graphics.drawable.Drawable
import android.graphics._
import com.waz.utils.returning
import com.waz.zclient.paintcode.WireStyleKit.{ResizingBehavior, drawDownArrow, drawServiceIcon}

trait WireDrawable extends Drawable {

  protected val paint = new Paint()

  override def setColorFilter(colorFilter: ColorFilter): Unit = paint.setColorFilter(colorFilter)

  override def getOpacity: Int = paint.getAlpha

  override def setAlpha(alpha: Int): Unit = paint.setAlpha(alpha)

  def setColor(color: Int): Unit = paint.setColor(color)
}

case class DownArrowDrawable() extends WireDrawable {
  override def draw(canvas: Canvas): Unit =
    drawDownArrow(canvas, new RectF(canvas.getClipBounds), ResizingBehavior.AspectFit, paint.getColor)
}

case class ServicePlaceholderDrawable(cornerRadius: Float = 0, backgroundColor: Int = Color.WHITE) extends WireDrawable {
  import ServicePlaceholderDrawable._

  val bgPaint = returning(new Paint())(_.setColor(backgroundColor))
  paint.setAlpha(8)

  override def draw(canvas: Canvas): Unit = {
    val b = canvas.getClipBounds
    val rect = new RectF(b)

    val w: Float = b.right - b.left
    val h: Float = b.bottom - b.top

    val wi = w * InnerSizeFactor
    val hi = h * InnerSizeFactor

    val li = w * (1f - InnerSizeFactor) / 2f
    val ti = h * (1f - InnerSizeFactor) / 2f
    val rectInner = new RectF(li, ti, li + wi, ti + hi)

    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
    drawServiceIcon(canvas, rectInner, ResizingBehavior.AspectFit, paint.getColor)
  }
}

object ServicePlaceholderDrawable {
  val InnerSizeFactor = 0.5f
}