package com.example.myapplication.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.viewmodel.AuthViewModel
import com.example.myapplication.viewmodel.CalculatorViewModel
import com.example.myapplication.viewmodel.ExpenseViewModel
import com.example.myapplication.viewmodel.FairViewModel
import com.example.myapplication.viewmodel.ProductViewModel
import com.example.myapplication.viewmodel.RequestedProductsViewModel
import com.example.myapplication.viewmodel.SalesViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    productViewModel: ProductViewModel = viewModel(),
    salesViewModel: SalesViewModel = viewModel(),
    expenseViewModel: ExpenseViewModel = viewModel(),
    fairViewModel: FairViewModel = viewModel(),
    calculatorViewModel: CalculatorViewModel = viewModel(),
    requestedProductsViewModel: RequestedProductsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    val userRole by authViewModel.userRole.collectAsState()
    val showCreatePinDialog by authViewModel.showCreatePinDialog.collectAsState()
    val hasAdminPinSet by authViewModel.hasAdminPinSet.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Lista"
                        )
                    },
                    label = { Text("Lista", fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Reporte"
                        )
                    },
                    label = { Text("Reporte", fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 2,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Ferias"
                        )
                    },
                    label = { Text("Ferias", fontWeight = if (pagerState.currentPage == 2) FontWeight.Bold else FontWeight.Normal) }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 3,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(3) }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Solicitados"
                        )
                    },
                    label = { Text("Encargos", fontWeight = if (pagerState.currentPage == 3) FontWeight.Bold else FontWeight.Normal) }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 4,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(4) }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "Calculadora"
                        )
                    },
                    label = { Text("Calculadora", fontWeight = if (pagerState.currentPage == 4) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 2,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) { page ->
            when (page) {
                0 -> ProductListScreen(
                    viewModel = productViewModel,
                    salesViewModel = salesViewModel,
                    authViewModel = authViewModel,
                    userRole = userRole,
                    onLogout = onLogout
                )
                1 -> SalesReportScreen(
                    salesViewModel = salesViewModel,
                    productViewModel = productViewModel,
                    expenseViewModel = expenseViewModel,
                    userRole = userRole
                )
                2 -> FairScreen(
                    fairViewModel = fairViewModel,
                    salesViewModel = salesViewModel,
                    expenseViewModel = expenseViewModel,
                    userRole = userRole
                )
                3 -> RequestedProductsScreen(
                    viewModel = requestedProductsViewModel
                )
                4 -> CalculatorScreen(viewModel = calculatorViewModel)
            }
        }
    }

    if (showCreatePinDialog) {
        CreatePinDialog(
            isFirstLaunch = !hasAdminPinSet,
            onCreatePin = { pin -> authViewModel.createAdminPin(pin) },
            onDismiss = { authViewModel.closeCreatePinDialog() }
        )
    }
}
