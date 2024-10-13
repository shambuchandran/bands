package com.example.bands.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.bands.di.BandsViewModel
import com.example.bands.utils.CommonImage

enum class State {
    INITIAL, ACTIVE, COMPLETED
}

@Composable
fun SingleStatusScreen(
    navController: NavHostController,
    viewModel: BandsViewModel,
    userId: String
) {
    val statuses = viewModel.status.collectAsState().value.filter {
        it.user.userId == userId
    }
    if (statuses.isNotEmpty()) {
        var currentStatus by remember {
            mutableStateOf(0)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            CommonImage(
                data = statuses[currentStatus].imageUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                statuses.forEachIndexed { index, status ->
                    CustomProgressIndicator(
                        modifier = Modifier
                            .weight(1f)
                            .height(7.dp)
                            .padding(1.dp),
                        //state = if (currentStatus.value < index) State.INITIAL else if (currentStatus.value == index) State.ACTIVE else State.COMPLETED
                        state = when {
                            currentStatus < index -> State.INITIAL
                            currentStatus == index -> State.ACTIVE
                            else -> State.COMPLETED
                        }, onComplete = {
                            if (currentStatus < statuses.size - 1) currentStatus++ else {
                                navController.popBackStack()
                            }
                        }
                    )
//                    {
//                        if (currentStatus < statuses.size-1) currentStatus ++ else{
//                            navController.popBackStack()
//                        }
//                    }
                }
            }
            Button(onClick = {viewModel.removeStatus(currentStatus)
                if (currentStatus>=statuses.size-1) navController.popBackStack()
            },modifier = Modifier.padding(16.dp)) {
                Text("Delete")
            }
        }
    }


}

@Composable
fun CustomProgressIndicator(modifier: Modifier, state: State, onComplete: () -> Unit) {
    //var progress = if (state == State.INITIAL) 0f else 1f
    var progress by remember { mutableStateOf(if (state == State.INITIAL) 0f else 1f) }
    if (state == State.ACTIVE) {
        val toggleState = remember {
            mutableStateOf(false)
        }
        LaunchedEffect(toggleState) {
            toggleState.value = true
        }
        val animatedProgress: Float by animateFloatAsState(
            if (toggleState.value) 1f else 0f,
            animationSpec = tween(4000),
            finishedListener = { onComplete.invoke() }, label = ""
        )
        progress = animatedProgress
    }
    LinearProgressIndicator(modifier = modifier, color = Color.Gray, progress = progress)

}