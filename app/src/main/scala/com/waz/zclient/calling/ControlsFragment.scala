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

import android.content.Context
import android.os.Bundle
import android.support.annotation.Nullable
import android.support.v4.app.Fragment
import android.view._
import android.widget.TextView
import com.waz.ZLog.ImplicitTag.implicitLogTag
import com.waz.ZLog.verbose
import com.waz.service.ZMessaging.clock
import com.waz.utils.events.{ClockSignal, Signal, Subscription}
import com.waz.utils.{returning, _}
import com.waz.zclient.calling.controllers.CallController
import com.waz.zclient.calling.views.{CallingHeader, CallingMiddleLayout, ControlsView}
import com.waz.zclient.utils.RichView
import com.waz.zclient.{FragmentHelper, R}
import org.threeten.bp.Instant

import scala.concurrent.duration._

class ControlsFragment extends FragmentHelper {

  implicit def ctx: Context = getActivity

  private lazy val controller = inject[CallController]

  private lazy val degradedWarningTextView = returning(view[TextView](R.id.degraded_warning)) { vh =>
    controller.convDegraded.onUi(degraded => vh.foreach(_.setVisible(degraded)))
    controller.degradationWarningText.onUi(text => vh.foreach(_.setText(text)))
  }

  private lazy val degradedConfirmationTextView = returning(view[TextView](R.id.degraded_confirmation)) { vh =>
    controller.convDegraded.onUi(degraded => vh.foreach(_.setVisible(degraded)))
    controller.degradationConfirmationText.onUi(text => vh.foreach(_.setText(text)))
  }

  private lazy val callingHeader   = view[CallingHeader](R.id.calling_header)
  private lazy val callingMiddle   = view[CallingMiddleLayout](R.id.calling_middle)
  private lazy val callingControls = view[ControlsView](R.id.controls_grid)
  private var subs = Set[Subscription]()

  private lazy val lastControlsClick = Signal[(Boolean, Instant)]() //true = show controls and set timer, false = hide controls

  private lazy val controlsVisible =
    (for {
      true         <- controller.isVideoCall
      Some(est)    <- controller.currentCall.map(_.estabTime)
      (show, last) <- lastControlsClick.orElse(Signal.const((true, clock.instant())))
      display <- if (show) ClockSignal(3.seconds).map(c => last.max(est).until(c).asScala <= 3.seconds) else Signal.const(false)
    } yield display).orElse(Signal.const(true))

  override def onCreateView(inflater: LayoutInflater, viewGroup: ViewGroup, savedInstanceState: Bundle): View =
    inflater.inflate(R.layout.fragment_calling_controls, viewGroup, false)

  override def onViewCreated(v: View, @Nullable savedInstanceState: Bundle): Unit = {
    super.onViewCreated(v, savedInstanceState)

    degradedWarningTextView
    degradedConfirmationTextView
    callingHeader
    callingMiddle
    callingControls

    controller.isCallActive.onUi {
      case false =>
        verbose("call no longer exists, finishing activity")
        getActivity.finish()
      case _ =>
    }

    controller.callConvId.onChanged.onUi(_ => restart())

    //ensure activity gets killed to allow content to change if the conv degrades (no need to kill activity on audio call)
    (for {
      degraded <- controller.convDegraded
      video    <- controller.isVideoCall
    } yield degraded && video).onChanged.filter(_ == true).onUi(_ => getActivity.finish())
  }

  override def onStart(): Unit = {
    super.onStart()

    lastControlsClick ! (true, clock.instant()) //reset timer after coming back from participants

    subs += controlsVisible.onUi {
      case true  => getView.fadeIn()
      case false => getView.fadeOut()
    }

    callingControls.foreach(controls =>
      subs += controls.onButtonClick.onUi { _ =>
        verbose("button clicked")
        lastControlsClick ! (true, clock.instant())
      }
    )

    //we need to listen to clicks on the outer layout, so that we can set this.getView to gone.
    getView.getParent.asInstanceOf[View].onClick {
      Option(getView).map(_.getVisibility != View.VISIBLE).foreach(lastControlsClick ! (_, clock.instant()))
    }

    callingMiddle.foreach(vh => subs += vh.onShowAllClicked.onUi { _ =>
      getFragmentManager.beginTransaction
        .setCustomAnimations(
          R.anim.fragment_animation_second_page_slide_in_from_right_no_alpha,
          R.anim.fragment_animation_second_page_slide_out_to_left_no_alpha,
          R.anim.fragment_animation_second_page_slide_in_from_left_no_alpha,
          R.anim.fragment_animation_second_page_slide_out_to_right_no_alpha)
        .replace(R.id.controls_layout, CallParticipantsFragment(), CallParticipantsFragment.Tag)
        .addToBackStack(CallParticipantsFragment.Tag)
        .commit
    })
  }

  override def onResume() = {
    super.onResume()
    controller.callControlsVisible ! true
  }


  override def onPause() = {
    controller.callControlsVisible ! false
    super.onPause()
  }

  override def onStop(): Unit = {
    getView.getParent.asInstanceOf[View].setOnClickListener(null)
    subs.foreach(_.destroy())
    subs = Set.empty
    super.onStop()
  }

  private def restart() = {
    verbose("restart")
    getActivity.finish()
    CallingActivity.start(ctx)
    getActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
  }

}

object ControlsFragment {
  val VideoViewTag = "VIDEO_VIEW_TAG"
  val Tag = classOf[ControlsFragment].getName

  def newInstance: Fragment = new ControlsFragment
}
