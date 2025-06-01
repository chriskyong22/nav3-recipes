package com.example.nav3recipes.scenes.materiallistdetail

/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.nav3recipes.content.ContentRed
import com.example.nav3recipes.content.ContentYellow
import kotlinx.serialization.Serializable

/**
 * This example uses the Material ListDetailSceneStrategy to create an adaptive scene. It has three
 * destinations: ConversationList, ConversationDetail and Profile. When the window width allows it,
 * the content for these destinations will be shown in a two pane layout.
 */
@Serializable
object ConversationList : NavKey

@Serializable
data class ConversationDetail(val id: String) : NavKey

@Serializable
data object Profile : NavKey

@Serializable
open class Routes : NavKey {
    @Serializable
    data object LandingScreen : Routes()
    @Serializable
    data object DetailScreen : Routes()
    @Serializable
    data object SecondListScreen : Routes()
    @Serializable
    data object SecondListDetailScreen : Routes()
    @Serializable
    data object ExternalScreen : Routes()
}

@Serializable
data object NestedGraphKey

@Serializable
data object SecondScreenNestedGraphViewModel

class MaterialListDetailActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val hostAppDestinations: (NavBackStack) -> (EntryProviderBuilder<Routes>.() -> Unit) =
                { backStack ->
                    {
                        entry<Routes.ExternalScreen>(
                            metadata = ListDetailSceneStrategy.listPane(
                                sceneKey = "External Screen",
                                detailPlaceholder = {
                                    ContentYellow("Choose a item from the list")
                                }
                            )
                        ) {
                            ContentRed("Welcome to External Screen") {

                            }
                        }
                    }
                }

            val backStack: NavBackStack = rememberNavBackStack<Routes>(Routes.LandingScreen)
            val listDetailStrategy = rememberListDetailSceneStrategy<Routes>()
            val savedStateNavEntryDecorator = rememberSavedStateNavEntryDecorator()
            val viewModelStoreNavEntryDecorator = rememberViewModelStoreNavEntryDecorator()
            val nestedMetaDataKey = "nestedMetaDataKey"
            val nestedGraphViewModelStoreNavEntryDecorator = rememberSharedViewModelStoreNavEntryDecorator(nestedMetaDataKey)
            NavDisplay(
                backStack = backStack as SnapshotStateList<Routes>,
                onBack = { keysToRemove -> repeat(keysToRemove) { backStack.removeLastOrNull() } },
                sceneStrategy = listDetailStrategy,
                entryDecorators = listOf(
                    savedStateNavEntryDecorator,
                    viewModelStoreNavEntryDecorator,
                    nestedGraphViewModelStoreNavEntryDecorator,
                ),
                entryProvider = entryProvider {
                    entry<Routes.LandingScreen>(
                        metadata = ListDetailSceneStrategy.listPane(
                            sceneKey = "General",
                            detailPlaceholder = {
                                ContentYellow("Choose a item from the list")
                            }
                        ),
                    ) {

                        ContentRed("Welcome to Landing Page") {
                            Button(
                                onClick = {
                                    backStack.add(Routes.SecondListScreen)
                                }
                            ) {
                                Text("Go to second list scene")
                            }
                            Button(onClick = {
                                backStack.add(Routes.DetailScreen)
                            }) {
                                Text("View detail")
                            }
                            Button(onClick = {
                                backStack.add(Routes.ExternalScreen)
                            }) {
                                Text("Go to External Screen")
                            }
                        }
                    }
                    entry<Routes.DetailScreen>(
                        metadata = ListDetailSceneStrategy.detailPane(sceneKey = "General"),
                    ) {
                        ContentRed("Welcome to first list detail screen") {

                        }
                    }
                    entry<Routes.SecondListScreen>(
                        metadata = ListDetailSceneStrategy.listPane(
                            sceneKey = "Second List Screen",
                            detailPlaceholder = {
                                ContentYellow("Choose a item from the second list")
                            }
                        ).plus(
                            nestedMetaDataKey to SecondScreenNestedGraphViewModel
                        ),
                    ) {
                        val viewModel: TestViewModel = viewModel(
                            viewModelStoreOwner = LocalSharedViewModelStoreOwner.current
                        )
                        Log.d("[SHARED VIEW MODEL]", viewModel.toString())
                        ContentRed("Welcome to request status") {
                            Button(onClick = {
                                backStack.add(Routes.SecondListDetailScreen)
                            }) {
                                Text("View second list item detail")
                            }
                        }
                    }
                    entry<Routes.SecondListDetailScreen>(
                        metadata = ListDetailSceneStrategy.detailPane(
                            sceneKey = "Second List Screen"
                        ).plus(
                            nestedMetaDataKey to SecondScreenNestedGraphViewModel
                        ),
                    ) {
                        val viewModel: TestViewModel = viewModel(
                            viewModelStoreOwner = LocalSharedViewModelStoreOwner.current
                        )
                        Log.d("[SHARED VIEW MODEL]", viewModel.toString())
                        ContentRed("Second List Item Detail") {

                        }
                    }

                    hostAppDestinations.invoke(backStack)()
                }
            )
        }
    }
}
