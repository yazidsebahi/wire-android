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
package com.waz.zclient.calling

import android.view.View
import com.waz.ZLog.ImplicitTag.implicitLogTag
import com.waz.ZLog.verbose
import com.waz.threading.{CancellableFuture, Threading}
import com.waz.utils.returning
import com.waz.zclient.FragmentHelper
import com.waz.zclient.utils.ViewUtils
import scala.concurrent.duration._

trait FadingControls extends FragmentHelper {
  private var inOrFadingIn = false
  private var tapFuture = Option.empty[CancellableFuture[Unit]]

  private var controls = Seq[View]()

  def setFadingControls(controls: View*): Unit = {
    this.controls = controls
  }

  protected def toggleControlVisibility(): Unit =
    if (inOrFadingIn) {
      fadeOut()
    } else {
      fadeIn()
      extendControlsDisplay()
    }

  protected def extendControlsDisplay(): Unit = {
    verbose(s"extendControlsDisplay")
    tapFuture.foreach(_.cancel())
    tapFuture = Option(
      returning(CancellableFuture.delay(3.seconds)) {
        _.foreach { _ => fadeOut() }(Threading.Ui)
      }
    )
  }

  protected def stopFadeOut(): Unit = {
    tapFuture.foreach(_.cancel())
    tapFuture = None
    fadeIn()
  }

  private def fadeIn(): Unit = {
    verbose(s"fadeIn")
    controls.foreach(ViewUtils.fadeInView)
    inOrFadingIn = true
  }

  private def fadeOut(): Unit = {
    verbose(s"fadeOut")
    controls.foreach(ViewUtils.fadeOutView)
    inOrFadingIn = false
  }
}
