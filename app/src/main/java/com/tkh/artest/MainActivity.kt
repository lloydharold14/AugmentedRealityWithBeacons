package com.tkh.artest

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity

@ExperimentalMaterial3Api
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ARNavigationApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARNavigationApp() {
    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(title = { Text("AR Navigation") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ARFragmentContainer()
        }
    }
}

@Composable
fun ARFragmentContainer() {
    val context = LocalContext.current
    val fragmentManager = (context as FragmentActivity).supportFragmentManager

    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).apply {
                id = View.generateViewId()
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            if (fragmentManager.findFragmentById(view.id) == null) {
                fragmentManager.beginTransaction()
                    .replace(view.id, ARNavigationPredefinedFragment.newInstance())
                    .commit()
            }
        }
    )
}