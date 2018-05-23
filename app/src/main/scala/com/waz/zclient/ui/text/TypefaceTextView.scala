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
  *//**
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
package com.waz.zclient.ui.text

import android.content.Context
import android.content.res.{Resources, TypedArray}
import android.support.v7.widget.AppCompatTextView
import android.text.TextUtils
import android.util.AttributeSet
import com.waz.zclient.common.controllers.ThemeController.Theme
import com.waz.zclient.common.controllers.{ThemeController, ThemedView}
import com.waz.zclient.ui.text.TypefaceTextView._
import com.waz.zclient.ui.utils.TypefaceUtils
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.{R, ViewHelper}

class TypefaceTextView(val context: Context, val attrs: AttributeSet, val defStyle: Int) extends AppCompatTextView(context, attrs, defStyle) with ViewHelper with ThemedView {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null)

  private var transform = ""

  val a: TypedArray = context.getTheme.obtainStyledAttributes(attrs, R.styleable.TypefaceTextView, 0, 0)
  val font: String = a.getString(R.styleable.TypefaceTextView_w_font)
  val themedColor: Option[Int] = Option(a.getInt(R.styleable.TypefaceTextView_themedColor, 0)).filter(_ != 0)
  if (!TextUtils.isEmpty(font)) setTypeface(font)
  transform = a.getString(R.styleable.TypefaceTextView_transform)
  if (!TextUtils.isEmpty(transform) && getText != null) setTransformedText(getText.toString, transform)
  a.recycle()
  setSoundEffectsEnabled(false)

  private var forcedTheme = Option.empty[Resources#Theme]

  private def setTheme(theme: Resources#Theme): Unit = {
    val t = forcedTheme.getOrElse(theme)
    val colorId = themedColor.collect {
      case PrimaryColorIndex => R.attr.wirePrimaryTextColor
      case SecondaryColorIndex => R.attr.wireSecondaryTextColor
    }.getOrElse(R.attr.wirePrimaryTextColor)

    setTextColor(getStyledColor(colorId, t, getCurrentTextColor)(context))
  }

  override def setTheme(theme: Theme): Unit = setTheme(inject[ThemeController].getTheme(theme))

  def forceTheme(theme: Option[Resources#Theme]): Unit = {
    forcedTheme = theme
    setTheme(currentTheme.getOrElse(Theme.Light))
  }

  def setTypeface(font: String): Unit = setTypeface(TypefaceUtils.getTypeface(font))

  def setTransform(transform: String): Unit = this.transform = transform

  def setTransformedText(text: String, transform: String): Unit = {
    val transformer = TextTransform.get(transform)
    this.setText(transformer.transform(text))
  }

  def setTransformedText(text: String): Unit = {
    val transformer = TextTransform.get(this.transform)
    this.setText(transformer.transform(text))
  }
}

object TypefaceTextView {
  val PrimaryColorIndex = 1
  val SecondaryColorIndex = 2
}
