package com.shieldguard.app.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shieldguard.app.data.local.entity.*
import com.shieldguard.app.ui.components.GlassButton
import com.shieldguard.app.ui.components.GlassCard
import com.shieldguard.app.ui.components.GlassSegmentedControl
import com.shieldguard.app.ui.components.GlassySwitch
import com.shieldguard.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(viewModel: RulesViewModel) {
    val rules by viewModel.rules.collectAsState()
    val allowlist by viewModel.allowlist.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var showAddAllowlistDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundObsidian)
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopStart)
                .offset(x = (-60).dp, y = (-40).dp)
                .background(Brush.radialGradient(listOf(CyberCyanGlow, Color.Transparent)))
        )

        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                // Sleek Glass Floating Action Button
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(listOf(CyberCyan, ElectricIndigo))
                        )
                        .border(1.dp, GlassBorderSpecular, RoundedCornerShape(24.dp))
                        .clickable {
                            if (selectedTab == 0) showAddRuleDialog = true
                            else showAddAllowlistDialog = true
                        }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = BackgroundObsidian,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedTab == 0) "Add Rule" else "Add VIP Entry",
                            style = MaterialTheme.typography.labelLarge,
                            color = BackgroundObsidian,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Title Header
                Text(
                    text = "FIREWALL RULES",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = CyberCyan
                )
                Text(
                    text = "Rule Configuration",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Apple-style Glass Segmented Control
                GlassSegmentedControl(
                    items = listOf("Firewall Rules (${rules.size})", "VIP Allowlist (${allowlist.size})"),
                    selectedIndex = selectedTab,
                    onItemSelected = { selectedTab = it }
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (selectedTab == 0) {
                    RulesListTab(
                        rules = rules,
                        onToggle = { viewModel.toggleRule(it) },
                        onDelete = { viewModel.deleteRule(it.id) }
                    )
                } else {
                    AllowlistTab(
                        allowlist = allowlist,
                        onDelete = { viewModel.deleteAllowlistEntry(it.id) }
                    )
                }
            }

            if (showAddRuleDialog) {
                AddRuleGlassDialog(
                    onDismiss = { showAddRuleDialog = false },
                    onAdd = { pattern, type, desc ->
                        viewModel.addCustomRule(pattern, type, desc)
                        showAddRuleDialog = false
                    }
                )
            }

            if (showAddAllowlistDialog) {
                AddAllowlistGlassDialog(
                    onDismiss = { showAddAllowlistDialog = false },
                    onAdd = { value, type, note ->
                        viewModel.addAllowlistEntry(value, type, note)
                        showAddAllowlistDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun RulesListTab(
    rules: List<RuleEntity>,
    onToggle: (RuleEntity) -> Unit,
    onDelete: (RuleEntity) -> Unit
) {
    if (rules.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No active firewall rules. Tap '+ Add Rule' below.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(rules, key = { it.id }) { rule ->
                RuleGlassCard(
                    rule = rule,
                    onToggle = { onToggle(rule) },
                    onDelete = { onDelete(rule) }
                )
            }
        }
    }
}

@Composable
private fun RuleGlassCard(
    rule: RuleEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val typeBadgeColor = when (rule.type) {
        RuleType.CALL_REGEX -> CyberCyan
        RuleType.SMS_KEYWORD, RuleType.SMS_REGEX -> NeonEmerald
        RuleType.NOTIFICATION_KEYWORD, RuleType.NOTIFICATION_REGEX -> WhatsAppNeon
        RuleType.WHITELIST_REGEX -> ElectricIndigo
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = GlassFillStandard,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(typeBadgeColor.copy(alpha = 0.15f))
                            .border(0.5.dp, typeBadgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = rule.type.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = typeBadgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (rule.action == RuleAction.ALLOW) "WHITELIST" else "BLOCK",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (rule.action == RuleAction.ALLOW) NeonEmerald else GmailCoral,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = rule.description.ifBlank { rule.pattern },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = rule.pattern,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Glassy Toggle
                GlassySwitch(
                    checked = rule.isEnabled,
                    onCheckedChange = { onToggle() }
                )

                if (rule.isUserCreated) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = GmailCoral,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllowlistTab(
    allowlist: List<AllowlistEntity>,
    onDelete: (AllowlistEntity) -> Unit
) {
    if (allowlist.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "VIP Allowlist is empty. Add your trusted contacts or bank headers.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(allowlist, key = { it.id }) { item ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = GlassFillStandard,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NeonEmerald.copy(alpha = 0.15f))
                                        .border(0.5.dp, NeonEmerald.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = item.type.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.value,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (item.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.note,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(onClick = { onDelete(item) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove",
                                tint = GmailCoral,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddRuleGlassDialog(
    onDismiss: () -> Unit,
    onAdd: (pattern: String, type: RuleType, description: String) -> Unit
) {
    var pattern by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(RuleType.CALL_REGEX) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundSurface,
        title = {
            Text("Add Custom Firewall Rule", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Regex Pattern / Keyword") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = GlassBorderHairline,
                        focusedLabelColor = CyberCyan,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (e.g. Robocall Series)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = GlassBorderHairline,
                        focusedLabelColor = CyberCyan,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Channel Target:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == RuleType.CALL_REGEX,
                        onClick = { selectedType = RuleType.CALL_REGEX },
                        label = { Text("Calls") }
                    )
                    FilterChip(
                        selected = selectedType == RuleType.SMS_KEYWORD,
                        onClick = { selectedType = RuleType.SMS_KEYWORD },
                        label = { Text("SMS") }
                    )
                    FilterChip(
                        selected = selectedType == RuleType.NOTIFICATION_KEYWORD,
                        onClick = { selectedType = RuleType.NOTIFICATION_KEYWORD },
                        label = { Text("App Alert") }
                    )
                }
            }
        },
        confirmButton = {
            GlassButton(
                onClick = { if (pattern.isNotBlank()) onAdd(pattern, selectedType, description) },
                modifier = Modifier.height(44.dp)
            ) {
                Text("Save Rule", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun AddAllowlistGlassDialog(
    onDismiss: () -> Unit,
    onAdd: (value: String, type: AllowlistType, note: String) -> Unit
) {
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundSurface,
        title = {
            Text("Add to VIP Allowlist", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Phone Number or Header (e.g. HDFCBK)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonEmerald,
                        unfocusedBorderColor = GlassBorderHairline,
                        focusedLabelColor = NeonEmerald,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (e.g. Primary Bank / Family)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonEmerald,
                        unfocusedBorderColor = GlassBorderHairline,
                        focusedLabelColor = NeonEmerald,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            GlassButton(
                onClick = {
                    if (value.isNotBlank()) {
                        val type = if (value.any { it.isLetter() }) AllowlistType.SENDER_HEADER else AllowlistType.PHONE_NUMBER
                        onAdd(value, type, note)
                    }
                },
                containerColor = NeonEmerald,
                modifier = Modifier.height(44.dp)
            ) {
                Text("Allow Sender", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
