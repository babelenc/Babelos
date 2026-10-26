package com.example.ui.finance

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BabelRepository
import com.example.model.FinancialAccount
import com.example.model.FinancialBudget
import com.example.model.FinancialTransaction
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    repository: BabelRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val accounts by repository.observeAccounts().collectAsState(initial = emptyList())
    val transactions by repository.observeTransactions().collectAsState(initial = emptyList())
    val profile by repository.observeUserProfile().collectAsState(initial = null)

    val currency = profile?.currency ?: "INR"
    val currentMonthStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        sdf.format(Date())
    }
    val todayDateStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Date())
    }

    val budget by repository.observeBudget(currentMonthStr).collectAsState(initial = null)

    var selectedTab by remember { mutableStateOf("Overview") } // Overview, Ledger, Budget, Export
    var showAddTxnDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }

    // Month Calculations
    val monthTransactions = remember(transactions, currentMonthStr) {
        transactions.filter { it.date.startsWith(currentMonthStr) }
    }
    val totalIncome = remember(monthTransactions) {
        monthTransactions.filter { it.type == "Income" }.sumOf { it.amount }
    }
    val totalExpenses = remember(monthTransactions) {
        monthTransactions.filter { it.type == "Expense" }.sumOf { it.amount }
    }
    val netCashFlow = totalIncome - totalExpenses

    val totalTrackedBalance = remember(accounts) {
        accounts.sumOf { it.currentBalance }
    }

    // Expense breakdown by category
    val categoryExpenses = remember(monthTransactions) {
        monthTransactions.filter { it.type == "Expense" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    Scaffold(
        containerColor = BabelDarkBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Personal Finance",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = BabelTextPrimary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { showAddAccountDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BabelDarkSurfaceVariant)
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = "Add Account", tint = BabelCyan, modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = { showAddTxnDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BabelCyan)
                                .testTag("add_transaction_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Transaction", tint = Color(0xFF001F29), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Overview, Ledger, Budget, Export
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BabelDarkSurfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Overview", "Ledger", "Budget", "Export").forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BabelCyan else Color.Transparent)
                                .clickable { selectedTab = tab }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                color = if (isSelected) Color(0xFF001F29) else BabelTextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        ) {
            when (selectedTab) {
                "Overview" -> {
                    // Total Tracked Balances Cards
                    item {
                        BabelCard {
                            Text("TOTAL TRACKED LIQUID BALANCE", style = MaterialTheme.typography.labelSmall, color = BabelTextSecondary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currency ${"%,.2f".format(totalTrackedBalance)}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = BabelTextPrimary
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // Accounts row
                            Text("ACCOUNTS & LEDGERS", style = MaterialTheme.typography.labelSmall, color = BabelCyan, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (accounts.isEmpty()) {
                                Text("No accounts configured yet. Tap '+' to create Cash or Bank Account.", style = MaterialTheme.typography.bodySmall, color = BabelTextMuted)
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    accounts.forEach { acc ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(BabelDarkSurfaceVariant)
                                                .padding(12.dp)
                                        ) {
                                            Column {
                                                Text(acc.name, style = MaterialTheme.typography.labelMedium, color = BabelTextSecondary)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "$currency ${"%,.0f".format(acc.currentBalance)}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BabelTextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Monthly Cashflow Card
                    item {
                        BabelCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("OCTOBER 2026 CASH FLOW", style = MaterialTheme.typography.labelSmall, color = BabelCyan, fontWeight = FontWeight.Bold)
                                BabelBadge(
                                    text = if (netCashFlow >= 0) "Surplus" else "Deficit",
                                    color = if (netCashFlow >= 0) BabelSuccess else BabelError
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Recorded Income", style = MaterialTheme.typography.labelSmall, color = BabelTextMuted)
                                    Text("$currency ${"%,.2f".format(totalIncome)}", style = MaterialTheme.typography.titleMedium, color = BabelSuccess, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Recorded Expenses", style = MaterialTheme.typography.labelSmall, color = BabelTextMuted)
                                    Text("$currency ${"%,.2f".format(totalExpenses)}", style = MaterialTheme.typography.titleMedium, color = BabelError, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Net Cash Flow", style = MaterialTheme.typography.labelSmall, color = BabelTextMuted)
                                    Text("$currency ${"%,.2f".format(netCashFlow)}", style = MaterialTheme.typography.titleMedium, color = if (netCashFlow >= 0) BabelSuccess else BabelError, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Expense Breakdown
                    item {
                        BabelCard {
                            Text("SPENDING BY CATEGORY", style = MaterialTheme.typography.labelSmall, color = BabelCyan, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))

                            if (categoryExpenses.isEmpty()) {
                                Text("No expenses recorded this month.", style = MaterialTheme.typography.bodySmall, color = BabelTextMuted)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    categoryExpenses.forEach { (cat, amount) ->
                                        val percent = if (totalExpenses > 0) ((amount / totalExpenses) * 100).toInt() else 0
                                        Column {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(cat, style = MaterialTheme.typography.bodySmall, color = BabelTextPrimary)
                                                Text("$currency ${"%,.0f".format(amount)} ($percent%)", style = MaterialTheme.typography.bodySmall, color = BabelTextSecondary, fontWeight = FontWeight.SemiBold)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            BabelProgressBar(progress = (amount / totalExpenses.coerceAtLeast(1.0)).toFloat())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "Ledger" -> {
                    // Full Transactions List
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Transaction History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BabelTextPrimary)
                            Text("${transactions.size} records", style = MaterialTheme.typography.bodySmall, color = BabelTextMuted)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (transactions.isEmpty()) {
                        item {
                            BabelEmptyState(
                                icon = Icons.Default.ReceiptLong,
                                title = "No Transactions Recorded",
                                description = "Add your daily expenses, salary or apprenticeship stipends to start tracking your money.",
                                actionLabel = "Add Transaction",
                                onAction = { showAddTxnDialog = true }
                            )
                        }
                    } else {
                        items(transactions.sortedByDescending { it.date }, key = { it.id }) { txn ->
                            BabelCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (txn.type) {
                                                        "Income" -> BabelSuccess.copy(alpha = 0.15f)
                                                        "Transfer" -> BabelBlue.copy(alpha = 0.15f)
                                                        else -> BabelError.copy(alpha = 0.15f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (txn.type) {
                                                    "Income" -> Icons.Default.ArrowDownward
                                                    "Transfer" -> Icons.Default.SyncAlt
                                                    else -> Icons.Default.ArrowUpward
                                                },
                                                contentDescription = null,
                                                tint = when (txn.type) {
                                                    "Income" -> BabelSuccess
                                                    "Transfer" -> BabelBlue
                                                    else -> BabelError
                                                },
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = txn.category,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = BabelTextPrimary
                                            )
                                            Text(
                                                text = "${txn.date} • ${txn.paymentMethod}${if (txn.description.isNotBlank()) " • " + txn.description else ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = BabelTextSecondary
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${if (txn.type == "Income") "+" else if (txn.type == "Transfer") "⇄ " else "-"} $currency ${"%,.2f".format(txn.amount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when (txn.type) {
                                            "Income" -> BabelSuccess
                                            "Transfer" -> BabelBlue
                                            else -> BabelTextPrimary
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                "Budget" -> {
                    // Monthly Budget Configuration & Utilization
                    val budgetLimit = budget?.monthlyExpenseBudget ?: 15000.0
                    val savingsGoal = budget?.monthlySavingsTarget ?: 5000.0
                    val utilizedPercent = ((totalExpenses / budgetLimit) * 100).toInt()

                    item {
                        BabelCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("MONTHLY EXPENSE LIMIT", style = MaterialTheme.typography.labelSmall, color = BabelCyan, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { showEditBudgetDialog = true }) {
                                    Text("Edit Limits", color = BabelCyan, style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            Text(
                                text = "$currency ${"%,.0f".format(totalExpenses)} / $currency ${"%,.0f".format(budgetLimit)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = BabelTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            BabelProgressBar(
                                progress = (totalExpenses / budgetLimit).toFloat().coerceIn(0f, 1f),
                                color = if (totalExpenses > budgetLimit) BabelError else BabelCyan
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (totalExpenses > budgetLimit) "Warning: Monthly budget limit exceeded by $currency ${"%,.0f".format(totalExpenses - budgetLimit)}!"
                                else "$currency ${"%,.0f".format(budgetLimit - totalExpenses)} remaining safe spending for this month.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (totalExpenses > budgetLimit) BabelError else BabelTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        BabelCard {
                            Text("MONTHLY SAVINGS TARGET", style = MaterialTheme.typography.labelSmall, color = BabelCyan, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$currency ${"%,.0f".format(netCashFlow.coerceAtLeast(0.0))} / $currency ${"%,.0f".format(savingsGoal)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = BabelSuccess
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            BabelProgressBar(
                                progress = (netCashFlow.coerceAtLeast(0.0) / savingsGoal).toFloat().coerceIn(0f, 1f),
                                color = BabelSuccess
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Savings are generated when net income exceeds expenses.",
                                style = MaterialTheme.typography.labelSmall,
                                color = BabelTextMuted
                            )
                        }
                    }
                }
                "Export" -> {
                    // Data Export (CSV & JSON)
                    item {
                        BabelCard {
                            Text("DATA EXPORT & BACKUP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BabelTextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Export your financial ledger to CSV or JSON for spreadsheets, personal auditing, or local backup.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BabelTextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val csvContent = buildString {
                                        appendLine("Date,Type,Category,Amount,Currency,Account,PaymentMethod,Description")
                                        transactions.forEach { txn ->
                                            appendLine("${txn.date},${txn.type},${txn.category},${txn.amount},$currency,${txn.accountId},${txn.paymentMethod},\"${txn.description}\"")
                                        }
                                    }
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, csvContent)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Export BabelOS Finance CSV")
                                    context.startActivity(shareIntent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BabelCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("export_csv_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF001F29))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export Transactions to CSV", color = Color(0xFF001F29), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Add Transaction Dialog
    if (showAddTxnDialog) {
        var type by remember { mutableStateOf("Expense") } // Expense, Income, Transfer
        var amountStr by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Food") }
        var dateStr by remember { mutableStateOf(todayDateStr) }
        var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
        var paymentMethod by remember { mutableStateOf("Cash") }
        var description by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val incomeCategories = listOf("Salary", "Freelance", "Apprenticeship", "Family support", "Other")
        val expenseCategories = listOf("Food", "Rent", "Transport", "Education", "Phone & Internet", "Household", "Personal", "Health", "Other")

        AlertDialog(
            onDismissRequest = { showAddTxnDialog = false },
            title = { Text("New Transaction", fontWeight = FontWeight.Bold, color = BabelTextPrimary) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Type selector
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(BabelDarkSurfaceVariant).padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Expense", "Income", "Transfer").forEach { t ->
                            val isSel = type == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) (if (t == "Income") BabelSuccess else if (t == "Transfer") BabelBlue else BabelError) else Color.Transparent)
                                    .clickable {
                                        type = t
                                        category = if (t == "Income") "Salary" else if (t == "Transfer") "Self Transfer" else "Food"
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(t, color = if (isSel) Color.White else BabelTextSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount ($currency) *") },
                        modifier = Modifier.fillMaxWidth().testTag("transaction_amount_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category (e.g. Food, Salary, Rent)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { paymentMethod = it },
                        label = { Text("Payment Method (Cash, UPI, Card)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (errorMessage != null) {
                        Text(errorMessage!!, color = BabelError, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull()
                        if (amount == null || amount <= 0.0) {
                            errorMessage = "Please enter a valid positive numeric amount."
                            return@Button
                        }
                        coroutineScope.launch {
                            repository.addTransaction(
                                FinancialTransaction(
                                    type = type,
                                    amount = amount,
                                    category = category,
                                    date = dateStr,
                                    accountId = accountId,
                                    paymentMethod = paymentMethod,
                                    description = description
                                )
                            )
                            showAddTxnDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BabelCyan),
                    modifier = Modifier.testTag("save_transaction_button")
                ) {
                    Text("Save Transaction", color = Color(0xFF001F29), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTxnDialog = false }) { Text("Cancel", color = BabelTextSecondary) }
            },
            containerColor = BabelDarkSurface
        )
    }

    // Add Account Dialog
    if (showAddAccountDialog) {
        var accName by remember { mutableStateOf("") }
        var accType by remember { mutableStateOf("Cash") }
        var openingBalStr by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("Add Manual Account", fontWeight = FontWeight.Bold, color = BabelTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = accName, onValueChange = { accName = it }, label = { Text("Account Name (e.g. Wallet, SBI Bank)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = accType, onValueChange = { accType = it }, label = { Text("Type (Cash, Bank account, Other)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = openingBalStr, onValueChange = { openingBalStr = it }, label = { Text("Opening Balance ($currency)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (accName.isNotBlank()) {
                            val bal = openingBalStr.toDoubleOrNull() ?: 0.0
                            coroutineScope.launch {
                                repository.addAccount(
                                    FinancialAccount(
                                        name = accName,
                                        accountType = accType,
                                        openingBalance = bal,
                                        currentBalance = bal
                                    )
                                )
                                showAddAccountDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BabelCyan),
                    enabled = accName.isNotBlank()
                ) {
                    Text("Create Account", color = Color(0xFF001F29), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showAddAccountDialog = false }) { Text("Cancel", color = BabelTextSecondary) } },
            containerColor = BabelDarkSurface
        )
    }

    // Edit Budget Dialog
    if (showEditBudgetDialog) {
        var expenseBudgetStr by remember { mutableStateOf((budget?.monthlyExpenseBudget ?: 15000.0).toInt().toString()) }
        var savingsTargetStr by remember { mutableStateOf((budget?.monthlySavingsTarget ?: 5000.0).toInt().toString()) }

        AlertDialog(
            onDismissRequest = { showEditBudgetDialog = false },
            title = { Text("Edit Monthly Budget Limits", fontWeight = FontWeight.Bold, color = BabelTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = expenseBudgetStr, onValueChange = { expenseBudgetStr = it }, label = { Text("Monthly Expense Budget ($currency)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = savingsTargetStr, onValueChange = { savingsTargetStr = it }, label = { Text("Monthly Savings Target ($currency)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val exp = expenseBudgetStr.toDoubleOrNull() ?: 15000.0
                        val sav = savingsTargetStr.toDoubleOrNull() ?: 5000.0
                        coroutineScope.launch {
                            repository.saveBudget(
                                FinancialBudget(
                                    monthlyExpenseBudget = exp,
                                    monthlySavingsTarget = sav,
                                    monthYear = currentMonthStr
                                )
                            )
                            showEditBudgetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BabelCyan)
                ) {
                    Text("Update", color = Color(0xFF001F29), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showEditBudgetDialog = false }) { Text("Cancel", color = BabelTextSecondary) } },
            containerColor = BabelDarkSurface
        )
    }
}
