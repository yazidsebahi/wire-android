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
package com.waz.zclient.calling.views

import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.Gravity
import android.widget.{FrameLayout, LinearLayout}
import com.waz.utils.returning
import com.waz.zclient.calling.views.CallControlButtonView.ButtonColor
import com.waz.zclient.paintcode.GenericStyleKitView
import com.waz.zclient.paintcode.StyleKitView.StyleKitDrawMethod
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.ui.theme.{OptionsDarkTheme, OptionsLightTheme}
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.utils.RichView
import com.waz.zclient.{R, ViewHelper}

import scala.util.Try

case class WireIconView()

class CallControlButtonView(val context: Context, val attrs: AttributeSet, val defStyleAttr: Int) extends LinearLayout(context, attrs, defStyleAttr) with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null)

  inflate(R.layout.call_button_view)

  setOrientation(LinearLayout.VERTICAL)
  setGravity(Gravity.CENTER)
  setBackgroundColor(ContextCompat.getColor(getContext, R.color.transparent))

  private val iconDimension = Try(context.getTheme.obtainStyledAttributes(attrs, R.styleable.CallControlButtonView, 0, 0)).toOption.map { a =>
    returning { a.getDimensionPixelSize(R.styleable.CallControlButtonView_iconDimension, 0) }(_ => a.recycle())
  }.filter(_ != 0)

  private var pressed: Boolean = false
  private var active: Boolean = true

  private val buttonBackground = findById[FrameLayout](R.id.icon_background)
  private val iconView = returning(findById[GenericStyleKitView](R.id.icon)) { icon =>
    iconDimension.foreach { size =>
      icon.getLayoutParams.height = size
      icon.getLayoutParams.width = size
    }
  }
  private val buttonLabelView = findById[TypefaceTextView](R.id.text)

  setButtonColors()

  def setButtonPressed(pressed: Boolean): Unit = if (this.pressed != pressed) {
    this.pressed = pressed
    setButtonColors()
  }

  def setButtonActive(active: Boolean): Unit = if (this.active != active) {
    this.active = active
    setButtonColors()
  }

  private def setButtonColors(): Unit = {
    if (!active) {
      iconView.setColor(getColor(R.color.graphite_64))
      buttonBackground.setBackground(ContextCompat.getDrawable(getContext, R.drawable.selector__icon_button__background__calling_disabled))
    } else if (pressed) {
      iconView.setColor(new OptionsLightTheme(getContext).getTextColorPrimarySelector.getDefaultColor)
      buttonBackground.setBackground(ContextCompat.getDrawable(getContext, R.drawable.selector__icon_button__background__calling_toggled))
    } else {
      iconView.setColor(new OptionsDarkTheme(getContext).getTextColorPrimarySelector.getDefaultColor)
      buttonBackground.setBackground(ContextCompat.getDrawable(getContext, R.drawable.selector__icon_button__background__calling))
    }
  }

  def setText(stringId: Int): Unit = buttonLabelView.setText(getResources.getText(stringId))

  def set(icon: StyleKitDrawMethod, labelStringId: Int, onClick: () => Unit, color: ButtonColor = ButtonColor.Transparent): Unit = {
    iconView.setOnDraw(icon)
    setText(labelStringId)
    setColor(color)
    this.onClick { if (active) onClick() }
  }

  import ButtonColor._
  def setColor(color: ButtonColor) = {

    val (drawable, textColor) = color match {
      case Green       => (R.drawable.selector__icon_button__background__green,   R.color.selector__icon_button__text_color__dark)
      case Red         => (R.drawable.selector__icon_button__background__red,     R.color.selector__icon_button__text_color__dark)
      case Transparent => (R.drawable.selector__icon_button__background__calling, R.color.wire__text_color_primary_dark_selector)
    }

    iconView.setColor(getColorStateList(textColor).getDefaultColor)
    buttonBackground.setBackground(getDrawable(drawable))
  }

}

object CallControlButtonView {

  object ButtonColor extends Enumeration {
    val Green, Red, Transparent = Value
  }
  type ButtonColor = ButtonColor.Value

}
