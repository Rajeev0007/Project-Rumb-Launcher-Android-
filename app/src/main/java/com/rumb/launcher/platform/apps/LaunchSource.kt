package com.rumb.launcher.platform.apps

import android.graphics.Rect
import android.os.Bundle

/**
 * Where a launch started on screen. [bounds] are window coordinates of the tapped icon and
 * [options] carries the ActivityOptions scale-up animation, so apps open from their icon.
 */
class LaunchSource(val bounds: Rect, val options: Bundle?)
