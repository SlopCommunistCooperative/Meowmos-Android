package io.github.slopcommunistcooperative.meowmos.ext

import io.github.slopcommunistcooperative.meowmos.MeowmosApp
import io.github.slopcommunistcooperative.meowmos.util.AppLanguage

/**
 * Get the string resources by the R.string.xx.string
 *
 * Resolved against a Context that follows the in-app language selection, so a
 * language switch takes effect without restarting the process.
 * @author Xeu<thankrain@qq.com>
 */
val Int.string get() = AppLanguage.localized(MeowmosApp.CONTEXT).getString(this)