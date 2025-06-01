package com.example.nav3recipes.scenes.materiallistdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

public object LocalSharedViewModelStoreOwner {
    private val LocalViewModelStoreOwner = compositionLocalOf<ViewModelStoreOwner?> { null }

    /**
     * Returns current composition local value for the owner or `null` if one has not been provided
     * nor is one available via [findViewTreeViewModelStoreOwner] on the current
     * [androidx.compose.ui.platform.LocalView].
     */
    public val current: ViewModelStoreOwner
        @Composable get() = LocalViewModelStoreOwner.current ?: throw IllegalStateException("Make sure to use the decorator")

    /**
     * Associates a [LocalViewModelStoreOwner] key to a value in a call to
     * [CompositionLocalProvider].
     */
    public infix fun provides(
        viewModelStoreOwner: ViewModelStoreOwner?
    ): ProvidedValue<ViewModelStoreOwner?> {
        return LocalViewModelStoreOwner.provides(viewModelStoreOwner)
    }
}