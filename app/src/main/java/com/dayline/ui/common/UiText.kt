package com.dayline.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * A message produced outside Compose (for example by a ViewModel) and translated only when
 * it is shown, so it follows the phone's language.
 */
class UiText(@StringRes val id: Int, vararg val args: Any) {
    @Composable
    fun resolve(): String = stringResource(id, *args)
}
