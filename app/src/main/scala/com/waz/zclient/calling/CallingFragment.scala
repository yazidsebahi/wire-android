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
import android.graphics.Color
import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.GridLayout
import com.waz.ZLog.ImplicitTag._
import com.waz.ZLog._
import com.waz.avs.{VideoPreview, VideoRenderer}
import com.waz.service.call.Avs.VideoReceiveState
import com.waz.utils.returning
import com.waz.zclient.calling.controllers.CallController
import com.waz.zclient.{FragmentHelper, R}

class VideoPreview2(context: Context) extends VideoPreview(context) {
  override def onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int): Unit = {
    setMeasuredDimension(View.getDefaultSize(getSuggestedMinimumWidth, widthMeasureSpec),
      View.getDefaultSize(getSuggestedMinimumHeight, heightMeasureSpec))
  }
}

class CallingFragment extends FragmentHelper {

  private lazy val controlsFragment = ControlsFragment.newInstance

  lazy val controller = inject[CallController]
  lazy val videoPreview = returning(new VideoPreview2(getContext)) { v =>
    controller.setVideoPreview(Some(v))
  }

  lazy val videoGrid = returning(view[GridLayout](R.id.video_grid)) { vh =>
    controller.videoReceiveState.map { vrs =>
      verbose(s"Got ${vrs.size} states")
      vrs.toSeq.filter(_._2.equals(VideoReceiveState.Started))
        .map( _._1)
        .map(userId => new VideoRenderer(getContext, userId.str, false))
    }.onUi { renderers =>
      verbose(s"Got ${renderers.size} renderers")
      vh.foreach { v =>
        verbose("Removing all views")
        v.removeAllViews()
        (videoPreview +: renderers).zipWithIndex.foreach { case (r, index) =>
          val (color, row, col) = index match {
            case 0 => (Color.CYAN, 0, 0)
            case 1 => (Color.BLUE, 0, 1)
            case 2 => (Color.GREEN, 1, 0)
            case 3 => (Color.MAGENTA, 1, 1)
          }
          val params = new GridLayout.LayoutParams()
          params.width = 0
          params.height = 0
          params.rowSpec = GridLayout.spec(row, 1, GridLayout.FILL, 1f)
          params.columnSpec = GridLayout.spec(col, 1, GridLayout.FILL, 1f)
          r.setLayoutParams(params)
          r.setBackgroundColor(color)
          v.addView(r)
        }
      }
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) =
    inflater.inflate(R.layout.fragment_calling_outer, container, false)

  override def onViewCreated(view: View, savedInstanceState: Bundle) = {
    super.onViewCreated(view, savedInstanceState)

    videoGrid

    getChildFragmentManager.beginTransaction
      .replace(R.id.controls_layout, controlsFragment, ControlsFragment.Tag)
      .commit
  }

  override def onBackPressed() = {
    withFragmentOpt(R.id.controls_layout) {
      case Some(f: FragmentHelper) if f.onBackPressed()               => true
      case Some(_) if getChildFragmentManager.popBackStackImmediate() => true
      case _ => super.onBackPressed()
    }
  }
}

object CallingFragment {
  val Tag = implicitLogTag
  def apply(): CallingFragment = new CallingFragment()
}
