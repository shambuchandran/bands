package com.example.bands.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bands.DestinationScreen
import com.example.bands.R
import com.example.bands.data.api.WeatherModel
import com.example.bands.di.BandsViewModel
import com.example.bands.di.NewsViewModel
import com.example.bands.di.WeatherViewModel
import com.example.bands.utils.CommonProgressBar
import com.example.bands.utils.CommonStatus
import com.example.bands.utils.CommonTitleText
import com.example.bands.utils.navigateTo
import com.example.bands.weatherupdates.NetworkResponse

@Composable
fun StatusScreen(navController: NavController, viewModel: BandsViewModel,weatherViewModel: WeatherViewModel) {
    val newsViewModel = NewsViewModel()
    LaunchedEffect(Unit) {
        viewModel.loadStatuses()
    }
    val weatherResult=weatherViewModel.weatherResult.observeAsState()

    val inProgressSts = viewModel.inProgressStatus.collectAsState().value
    val statuses = viewModel.status.collectAsState().value

    if (inProgressSts) {
        CommonProgressBar()
    } else {
        val userData = viewModel.userData.value
        val myStatus = statuses.filter { it.user.userId == userData?.userId }
        val othersStatus = statuses.filter { it.user.userId != userData?.userId }

        val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
                uri?.let {
                    viewModel.uploadStatus(uri)
                    viewModel.loadStatuses()
                }
            }
        val weatherData: WeatherModel? = when (val result = weatherResult.value)
        {
            is NetworkResponse.Error -> {
                null
            }
            NetworkResponse.Loading -> {
                null
            }
            is NetworkResponse.Success -> {
                result.data
            }
            null -> {
                null
            }
        }

        Scaffold(
//            floatingActionButton = {
//                FabStatus {
//                    launcher.launch("image/*")
//                }
//            },
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it)
                ) {
                    CommonTitleText(text = stringResource(R.string.happening),weatherData,showSearchBar = false)
                    if (statuses.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                //.weight(1f)
                                .height(108.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            AddStatusIcon {
                                launcher.launch("image/*")
                            }
                            VerticalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                            Text(text = "No status to watch",color = Color.White, modifier = Modifier.weight(1f))
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                if (myStatus.isNotEmpty()) {
                                    CommonStatus(
                                        imageUrl = myStatus[0].user.imageUrl,
                                        //name = myStatus[0].user.name
                                        name = "you"
                                    ) {
                                        navigateTo(
                                            navController,
                                            DestinationScreen.SingleStatus.createRoute(myStatus[0].user.userId!!)
                                        )
                                    }
                                } else {
                                    AddStatusIcon {
                                        launcher.launch("image/*")
                                    }
                                }
                            }

                            // Separator
                            item {
                                VerticalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                            }

                            // Other Users' Statuses
                            if (othersStatus.isNotEmpty()) {
                                val uniqueUsers = othersStatus.map { it.user }.toSet().toList()
                                items(uniqueUsers) { uniqueUser ->
                                    CommonStatus(
                                        imageUrl = uniqueUser.imageUrl,
                                        name = uniqueUser.name
                                    ) {
                                        navigateTo(
                                            navController,
                                            DestinationScreen.SingleStatus.createRoute(uniqueUser.userId!!)
                                        )
                                    }
                                }
                            } else {
                                item {
                                    Text(
                                        text = "No status to watch",
                                        color = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .padding(24.dp)
                                    )
                                }
                            }
                        }
                    }
                    Divider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Top Headlines",
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(
                                    colorResource(id = R.color.BgColor)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            color = Color.Black,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold
                        )
                        NewsSection(newsViewModel, navController)
                    }

                    BottomNavigationMenu(
                        selectedItem = BottomNavigationItem.STATUSLIST,
                        navController = navController
                    )
                }
            }
        )
    }
}

@Composable
fun AddStatusIcon(onAddClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center, modifier = Modifier.padding(8.dp)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onAddClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add Status",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        Text(text = "Add Status", color = Color.White, fontWeight = FontWeight.Bold,modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun VerticalDivider(modifier: Modifier = Modifier) {
    Divider(
        color = Color.Gray,
        modifier = modifier
            .width(1.dp)
            .height(94.dp)
    )
}

@Composable
fun FabStatus(onFabClick: () -> Unit) {
    FloatingActionButton(
        onClick = onFabClick,
        containerColor = MaterialTheme.colorScheme.secondary,
        shape = CircleShape,
        modifier = Modifier.padding(bottom = 78.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.AddCircle,
            contentDescription = "Add Status",
            tint = Color.White
        )

    }
}
