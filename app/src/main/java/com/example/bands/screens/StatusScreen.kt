package com.example.bands.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bands.di.BandsViewModel
import com.example.bands.utils.CommonDivider
import com.example.bands.utils.CommonProgressBar
import com.example.bands.utils.CommonRow
import com.example.bands.utils.CommonTitleText
import com.example.bands.DestinationScreen
import com.example.bands.utils.navigateTo


@Composable
fun StatusScreen(navController: NavController, viewModel: BandsViewModel) {
    LaunchedEffect(Unit) {
        viewModel.loadStatuses()
    }
    //val inProgressSts = viewModel.inProgressStatus.value
    val inProgressSts = viewModel.inProgressStatus.collectAsState().value
    val statuses = viewModel.status.collectAsState().value
    if (inProgressSts) {
        CommonProgressBar()
    } else {
        //val statuses = viewModel.status.value

        val userData = viewModel.userData.value
        val myStatus = statuses.filter { it.user.userId == userData?.userId }
        val othersStatus = statuses.filter { it.user.userId != userData?.userId }
        val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
                uri-> uri?.let {
                    viewModel.uploadStatus(uri)
                     viewModel.loadStatuses()
                }
        }

        Scaffold(floatingActionButton = {
            FabStatus {
                    launcher.launch("image/*")
            }
        }, content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                CommonTitleText(text = "Happening")
                if (statuses.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "No Statuses available")
                    }
                } else {
                    if (myStatus.isNotEmpty()) {
                        CommonRow(
                            imageUrl = myStatus[0].user.imageUrl,
                            name = myStatus[0].user.name
                        ) {
                            navigateTo(
                                navController,
                                DestinationScreen.SingleStatus.createRoute(myStatus[0].user.userId!!)
                            )
                        }
                        CommonDivider()
                        val uniqueUsers = othersStatus.map { it.user }.toSet().toList()
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            items(uniqueUsers) { uniqueUser ->
                                CommonRow(imageUrl = uniqueUser.imageUrl, name = uniqueUser.name) {
                                    navigateTo(
                                        navController,
                                        DestinationScreen.SingleStatus.createRoute(uniqueUser.userId!!)
                                    )
                                }
                            }
                        }
                    }
                }
                BottomNavigationMenu(
                    selectedItem = BottomNavigationItem.STATUSLIST,
                    navController = navController
                )
            }

        })
    }


}

@Composable
fun FabStatus(onFabClick: () -> Unit) {
    FloatingActionButton(
        onClick = onFabClick,
        containerColor = MaterialTheme.colorScheme.secondary,
        shape = CircleShape,
        modifier = Modifier.padding(bottom = 40.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = "Add Status",
            tint = Color.White
        )

    }
}