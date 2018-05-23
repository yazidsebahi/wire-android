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
import android.content.res.Resources
import android.util.AttributeSet
import android.view.Gravity
import android.widget.{FrameLayout, LinearLayout}
import com.waz.utils.returning
import com.waz.zclient.calling.views.CallControlButtonView.ButtonColor
import com.waz.zclient.common.controllers.{ThemeController, ThemedView}
import com.waz.zclient.paintcode.GenericStyleKitView
import com.waz.zclient.paintcode.StyleKitView.StyleKitDrawMethod
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.utils.RichView
import com.waz.zclient.{R, ViewHelper}
import com.waz.ZLog.ImplicitTag._
import com.waz.zclient.common.controllers.ThemeController.Theme

import scala.util.Try

class CallControlButtonView(val context: Context, val attrs: AttributeSet, val defStyleAttr: Int) extends LinearLayout(context, attrs, defStyleAttr) with ViewHelper with ThemedView {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null)

  val themeController = inject[ThemeController]

  private var otherColor = Option.empty[ButtonColor]

  inflate(R.layout.call_button_view)

  setOrientation(LinearLayout.VERTICAL)
  setGravity(Gravity.CENTER)

  private val iconDimension = Try(context.getTheme.obtainStyledAttributes(attrs, R.styleable.CallControlButtonView, 0, 0)).toOption.map { a =>
    returning { a.getDimensionPixelSize(R.styleable.CallControlButtonView_iconDimension, 0) }(_ => a.recycle())
  }.filter(_ != 0)

  private val buttonBackground = findById[FrameLayout](R.id.icon_background)
  private val iconView = returning(findById[GenericStyleKitView](R.id.icon)) { icon =>
    iconDimension.foreach { size =>
      icon.getLayoutParams.height = size
      icon.getLayoutParams.width = size
    }
  }
  private val buttonLabelView = findById[TypefaceTextView](R.id.text)

  override def setTheme(theme: Theme): Unit = {
    val resTheme = themeController.getTheme(theme)
    otherColor.fold {
      updateIconColor(resTheme)
      getStyledDrawable(R.attr.callButtonBackground, resTheme).foreach(buttonBackground.setBackground(_))
    } { color =>
      import ButtonColor._
      val (drawable, textColor) = color match {
        case Green       => (R.drawable.selector__icon_button__background__green,   R.color.selector__icon_button__text_color__dark)
        case Red         => (R.drawable.selector__icon_button__background__red,     R.color.selector__icon_button__text_color__dark)
      }
      iconView.setColor(getColorStateList(textColor).getDefaultColor)
      buttonBackground.setBackground(getDrawable(drawable))
    }
  }

  override def setEnabled(enabled: Boolean): Unit = {
    (0 until getChildCount).map(getChildAt(_)).foreach(_.setEnabled(enabled))
    super.setEnabled(enabled)
    updateIconColor(themeController.getTheme(currentTheme.getOrElse(Theme.Dark)))
  }

  override def setActivated(activated: Boolean): Unit = {
    super.setActivated(activated)
    updateIconColor(themeController.getTheme(currentTheme.getOrElse(Theme.Dark)))
  }

  private def updateIconColor(theme: Resources#Theme): Unit = {
    val iconColor =
      if (otherColor.isDefined) getColor(R.color.white)
      else if (!isEnabled) getStyledColor(R.attr.callIconDisabledColor, theme)
      else if (isActivated) getStyledColor(R.attr.wirePrimaryTextColorReverted, theme)
      else getStyledColor(R.attr.wirePrimaryTextColor, theme)
    iconView.setColor(iconColor)
  }

  def setText(stringId: Int): Unit = buttonLabelView.setText(getResources.getText(stringId))

  def set(icon: StyleKitDrawMethod, labelStringId: Int, onClick: () => Unit, forceColor: Option[ButtonColor] = None): Unit = {
    iconView.setOnDraw(icon)
    setText(labelStringId)
    otherColor = forceColor
    setTheme(currentTheme.getOrElse(Theme.Dark))
    this.onClick { onClick() }
  }

}

object CallControlButtonView {

  object ButtonColor extends Enumeration {
    val Green, Red = Value
  }
  type ButtonColor = ButtonColor.Value

}
