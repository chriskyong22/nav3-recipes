package com.example.nav3recipes.scenes.materiallistdetail

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.navEntryDecorator
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner

@Composable
internal fun shouldRemoveViewModelStoreCallback(): () -> Boolean {
    val activity = LocalActivity.current
    return { activity?.isChangingConfigurations != true }
}

@Composable
public fun rememberSharedViewModelStoreNavEntryDecorator(
    metaDataKey: String,
    viewModelStoreOwner: ViewModelStoreOwner =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        },
    shouldRemoveStoreOwner: () -> Boolean = shouldRemoveViewModelStoreCallback(),
): NavEntryDecorator<Any> = remember {
    ViewModelStoreNavEntryDecorator(metaDataKey, viewModelStoreOwner.viewModelStore, shouldRemoveStoreOwner)
}

/**
 * Provides the content of a [NavEntry] with a [ViewModelStoreOwner] and provides that
 * [ViewModelStoreOwner] as a [LocalViewModelStoreOwner] so that it is available within the content.
 *
 * This requires the usage of [androidx.navigation3.runtime.SavedStateNavEntryDecorator] to ensure
 * that the [NavEntry] scoped [ViewModel]s can properly provide access to
 * [androidx.lifecycle.SavedStateHandle]s
 *
 * @param [viewModelStore] The [ViewModelStore] that provides to NavEntries
 * @param [shouldRemoveStoreOwner] A lambda that returns a Boolean for whether the store owner for a
 *   [NavEntry] should be cleared when the [NavEntry] is popped from the backStack. If true, the
 *   entry's ViewModelStoreOwner will be removed.
 */
public fun ViewModelStoreNavEntryDecorator(
    metaDataKey: String,
    viewModelStore: ViewModelStore,
    shouldRemoveStoreOwner: () -> Boolean,
): NavEntryDecorator<Any> {
    val storeOwnerProvider: EntryViewModel = viewModelStore.getEntryViewModel()
    val onPop: (Any) -> Unit = { key ->
        if (shouldRemoveStoreOwner()) {
            storeOwnerProvider.clearViewModelStoreOwnerForKey(key)
        }
    }
    return navEntryDecorator(onPop) { entry ->
        val sharedViewModelStoreKey = entry.metadata[metaDataKey]
        val sharedViewModelStore = sharedViewModelStoreKey?.let {
            storeOwnerProvider.viewModelStoreForKey(
                entry.key, it,
            )
        }

        val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current

        val sharedViewModelStoreOwner = remember {
            sharedViewModelStore?.let { viewModelStore ->
                object :
                    ViewModelStoreOwner,
                    SavedStateRegistryOwner by savedStateRegistryOwner,
                    HasDefaultViewModelProviderFactory {
                    override val viewModelStore: ViewModelStore
                        get() = viewModelStore

                    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
                        get() = SavedStateViewModelFactory(null, savedStateRegistryOwner)

                    override val defaultViewModelCreationExtras: CreationExtras
                        get() =
                            MutableCreationExtras().also {
                                it[SAVED_STATE_REGISTRY_OWNER_KEY] = savedStateRegistryOwner
                                it[VIEW_MODEL_STORE_OWNER_KEY] = this
                            }

                    init {
                        require(this.lifecycle.currentState == Lifecycle.State.INITIALIZED) {
                            "The Lifecycle state is already beyond INITIALIZED. The " +
                                    "ViewModelStoreNavEntryDecorator requires adding the " +
                                    "SavedStateNavEntryDecorator to ensure support for " +
                                    "SavedStateHandles."
                        }
                        enableSavedStateHandles()
                    }
                }
            }
        }
        CompositionLocalProvider(
            LocalSharedViewModelStoreOwner provides sharedViewModelStoreOwner
        ) {
            entry.content.invoke(entry.key)
        }
    }
}

private class EntryViewModel : ViewModel() {
    private val mapToOwners = mutableMapOf<Any, Any>()
    private val owners = mutableMapOf<Any, Pair<MutableSet<Any>, ViewModelStore>>()

    fun viewModelStoreForKey(key: Any, sharedViewModelKey: Any): ViewModelStore {
        mapToOwners.getOrPut(key) { sharedViewModelKey }
        val pair = owners.getOrPut(sharedViewModelKey) { mutableSetOf<Any>() to ViewModelStore() }
        pair.first.add(key)
        owners[sharedViewModelKey] = pair
        return pair.second
    }

    fun clearViewModelStoreOwnerForKey(key: Any) {
        val ownersKey = mapToOwners[key]
        ownersKey?.let { ownerKey ->
            owners[ownerKey]?.let {
                it.first.remove(key)
                if (it.first.isEmpty()) {
                    owners.remove(ownerKey)?.second?.clear()
                }
            }
            mapToOwners.remove(key)
        }
    }

    override fun onCleared() {
        mapToOwners.clear()
        owners.forEach { (_, store) -> store.second.clear() }
    }
}

private fun ViewModelStore.getEntryViewModel(): EntryViewModel {
    val provider =
        ViewModelProvider(
            store = this,
            factory = viewModelFactory { initializer { EntryViewModel() } },
        )
    return provider[EntryViewModel::class]
}