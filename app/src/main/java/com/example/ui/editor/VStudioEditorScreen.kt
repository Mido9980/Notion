package com.example.ui.editor

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ProjectFile
import com.example.ui.theme.EditorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VStudioEditorScreen(
    viewModel: CodeEditorViewModel = viewModel()
) {
    val files by viewModel.files.collectAsState()
    val openedTabs by viewModel.openedTabs.collectAsState()
    val activeTabPath by viewModel.activeTabPath.collectAsState()
    val editorContent by viewModel.editorContent.collectAsState()
    val isExplorerOpen by viewModel.isExplorerOpen.collectAsState()
    val theme by viewModel.selectedTheme.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val replaceQuery by viewModel.replaceQuery.collectAsState()
    val isSearchVisible by viewModel.isSearchVisible.collectAsState()
    
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val isTerminalOpen by viewModel.isTerminalOpen.collectAsState()
    val isWebPreview by viewModel.isWebPreview.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var createIsFolder by remember { mutableStateOf(false) }
    var createNameInput by remember { mutableStateOf("") }
    
    var showRenameDialog by remember { mutableStateOf<String?>(null) }
    var renameInput by remember { mutableStateOf("") }

    var showThemeMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.sidebarBackground,
                    titleContentColor = theme.textColor,
                    navigationIconContentColor = theme.textColor,
                    actionIconContentColor = theme.textColor
                ),
                navigationIcon = {
                    IconButton(onClick = { viewModel.toggleExplorer() }) {
                        Icon(
                            imageVector = if (isExplorerOpen) Icons.Default.MenuOpen else Icons.Default.Menu,
                            contentDescription = "Toggle Drawer Explorer"
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "VStudio Logo",
                            tint = Color(0xFF007ACC),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VStudio Code",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Quick Formatting Auto-Wand
                    IconButton(
                        onClick = { viewModel.formatActiveCode() },
                        enabled = activeTabPath != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = "Format Code",
                            tint = if (activeTabPath != null) theme.keywordColor else Color.Gray
                        )
                    }

                    // Toggle Find and Replace Panel
                    IconButton(onClick = { viewModel.toggleSearchPanel() }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Find and Replace",
                            tint = if (isSearchVisible) theme.keywordColor else theme.textColor
                        )
                    }

                    // Play (Run) compiled simulations
                    IconButton(
                        onClick = { viewModel.runActiveCode() },
                        enabled = activeTabPath != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run Sandboxed Code",
                            tint = if (activeTabPath != null) Color(0xFF238636) else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Settings / Theme Picker
                    IconButton(onClick = { showThemeMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Editor Settings Theme"
                        )
                    }

                    // Themes Dropdown Choice
                    DropdownMenu(
                        expanded = showThemeMenu,
                        onDismissRequest = { showThemeMenu = false },
                        modifier = Modifier.background(theme.sidebarBackground)
                    ) {
                        DropdownMenuItem(
                            text = { Text("EDITOR COLOR THEME", color = theme.textColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            onClick = {},
                            enabled = false
                        )
                        
                        EditorTheme.allThemes.forEach { choice ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(choice.editorBackground, RoundedCornerShape(2.dp))
                                                .border(1.dp, Color.Gray, RoundedCornerShape(2.dp))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = choice.name,
                                            color = if (theme.name == choice.name) theme.keywordColor else theme.textColor,
                                            fontWeight = if (theme.name == choice.name) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.selectTheme(choice)
                                    showThemeMenu = false
                                }
                            )
                        }

                        Divider(color = theme.textColor.copy(alpha = 0.2f))

                        DropdownMenuItem(
                            text = { Text("FONT SIZE", color = theme.textColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            onClick = {},
                            enabled = false
                        )

                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Text Size: ${fontSize}sp", color = theme.textColor)
                                    Row {
                                        IconButton(
                                            onClick = { viewModel.adjustFontSize(false) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, "Smaller font", tint = theme.textColor, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { viewModel.adjustFontSize(true) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, "Larger font", tint = theme.textColor, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            },
                            onClick = {}
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(theme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Find and Replace Tray if active
                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    SearchReplacePanel(
                        theme = theme,
                        query = searchQuery,
                        replace = replaceQuery,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onReplaceChange = { viewModel.updateReplaceQuery(it) },
                        onReplaceAll = { viewModel.performReplaceAll() },
                        onClose = { viewModel.toggleSearchPanel() }
                    )
                }

                Row(modifier = Modifier.weight(1f)) {
                    // Sidebar File System Explorer Column
                    AnimatedVisibility(
                        visible = isExplorerOpen,
                        enter = slideInHorizontally() + fadeIn(),
                        exit = slideOutHorizontally() + fadeOut()
                    ) {
                        FileExplorerSidebar(
                            theme = theme,
                            files = files,
                            activePath = activeTabPath ?: "",
                            onOpenFile = { viewModel.openFile(it) },
                            onDelete = { viewModel.deleteFile(it) },
                            onRename = { old, current ->
                                renameInput = current
                                showRenameDialog = old
                            },
                            onCreateClick = { isDir ->
                                createIsFolder = isDir
                                createNameInput = ""
                                showCreateDialog = true
                            }
                        )
                    }

                    // Main Active Editor Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(theme.editorBackground)
                    ) {
                        // Open File Tabs Row
                        TabsRow(
                            theme = theme,
                            tabs = openedTabs,
                            activePath = activeTabPath,
                            onTabSelect = { viewModel.openFile(it) },
                            onCloseTab = { viewModel.closeTab(it) }
                        )

                        if (activeTabPath != null) {
                            TextAndCodeEditor(
                                theme = theme,
                                content = editorContent,
                                extension = activeTabPath?.substringAfterLast('.', "") ?: "",
                                fontSize = fontSize,
                                onContentChange = { viewModel.updateContent(it) },
                                onUndo = { viewModel.undo() },
                                onRedo = { viewModel.redo() }
                            )
                        } else {
                            // Empty State / Workspace Overview
                            EmptyWorkspaceView(theme = theme, onCreateFile = {
                                createIsFolder = false
                                createNameInput = ""
                                showCreateDialog = true
                            })
                        }
                    }
                }

                // Bottom Collapsible Compiler Runner Sandbox Terminal
                AnimatedVisibility(
                    visible = isTerminalOpen,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
                ) {
                    TerminalConsolePanel(
                        theme = theme,
                        output = terminalOutput,
                        isWeb = isWebPreview,
                        onClose = { viewModel.closeTerminal() }
                    )
                }
            }

            // Developer Quick Symbol toolbar floating bottom-right helper above keyboard when file open
            if (activeTabPath != null && !isTerminalOpen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                ) {
                    DeveloperSymbolToolbar(
                        theme = theme,
                        editorText = editorContent,
                        onInsert = { symbol ->
                            if (symbol == "Tab") {
                                viewModel.updateContent(editorContent + "    ")
                            } else {
                                viewModel.updateContent(editorContent + symbol)
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal Create dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = if (createIsFolder) "New Folder" else "New Code File",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = theme.textColor
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter relative workspace path below:",
                        fontSize = 13.sp,
                        color = theme.textColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = createNameInput,
                        onValueChange = { createNameInput = it },
                        placeholder = { Text(if (createIsFolder) "e.g., helpers" else "e.g., index.html, math.py") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.keywordColor,
                            unfocusedBorderColor = theme.textColor.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = theme.keywordColor),
                    onClick = {
                        if (createNameInput.isNotBlank()) {
                            viewModel.createFile(createNameInput, createIsFolder)
                        }
                        showCreateDialog = false
                    }
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = theme.textColor)
                }
            },
            containerColor = theme.sidebarBackground
        )
    }

    // Modal Rename dialog
    showRenameDialog?.let { fileToRename ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = {
                Text(
                    text = "Rename Item",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = theme.textColor
                )
            },
            text = {
                Column {
                    Text(
                        text = "Current: $fileToRename",
                        fontSize = 12.sp,
                        color = theme.textColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.keywordColor,
                            unfocusedBorderColor = theme.textColor.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = theme.keywordColor),
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renameFile(fileToRename, renameInput)
                        }
                        showRenameDialog = null
                    }
                ) {
                    Text("Rename", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("Cancel", color = theme.textColor)
                }
            },
            containerColor = theme.sidebarBackground
        )
    }
}

@Composable
fun SearchReplacePanel(
    theme: EditorTheme,
    query: String,
    replace: String,
    onQueryChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = theme.sidebarBackground),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, theme.textColor.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search text...", color = theme.textColor.copy(0.4f), fontSize = 13.sp) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.keywordColor,
                        unfocusedBorderColor = theme.textColor.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                OutlinedTextField(
                    value = replace,
                    onValueChange = onReplaceChange,
                    placeholder = { Text("Replace with...", color = theme.textColor.copy(0.4f), fontSize = 13.sp) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.keywordColor,
                        unfocusedBorderColor = theme.textColor.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = theme.keywordColor),
                    shape = RoundedCornerShape(4.dp),
                    onClick = onReplaceAll,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Replace All", color = Color.White, fontSize = 12.sp)
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Close Panel", tint = theme.textColor)
                }
            }
        }
    }
}

@Composable
fun FileExplorerSidebar(
    theme: EditorTheme,
    files: List<ProjectFile>,
    activePath: String,
    onOpenFile: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onCreateClick: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(theme.sidebarBackground)
            .drawBehind {
                drawLine(
                    color = theme.textColor.copy(alpha = 0.08f),
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Explorer title line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXPLORER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor,
                    fontFamily = FontFamily.Monospace
                )
                Row {
                    IconButton(
                        onClick = { onCreateClick(false) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NoteAdd,
                            contentDescription = "New File",
                            tint = theme.textColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { onCreateClick(true) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "New Folder",
                            tint = theme.textColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Divider(color = theme.textColor.copy(alpha = 0.06f))

            // File trees list
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(files) { file ->
                    val isSelected = file.path == activePath
                    val fileBg = if (isSelected) theme.background else Color.Transparent

                    var showContextMenu by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(fileBg)
                            .clickable {
                                if (file.isDirectory) {
                                    // Directories expand/collapse mocked safely through flat explorer list
                                } else {
                                    onOpenFile(file.path)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Depth indentation
                        val nIndent = file.path.count { it == '/' }
                        Spacer(modifier = Modifier.width((nIndent * 12).dp))

                        // File icons
                        FileIconResolver(file = file, theme = theme)

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = file.name,
                            fontSize = 14.sp,
                            color = if (isSelected) theme.keywordColor else theme.textColor,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // File operation dots
                        IconButton(
                            onClick = { showContextMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Asset menu",
                                tint = theme.textColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showContextMenu,
                            onDismissRequest = { showContextMenu = false },
                            modifier = Modifier.background(theme.sidebarBackground)
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Edit, "rename", tint = theme.textColor) },
                                text = { Text("Rename", color = theme.textColor) },
                                onClick = {
                                    showContextMenu = false
                                    onRename(file.path, file.name)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Delete, "delete", tint = Color.Red) },
                                text = { Text("Delete", color = Color.Red) },
                                onClick = {
                                    showContextMenu = false
                                    onDelete(file.path)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileIconResolver(file: ProjectFile, theme: EditorTheme) {
    if (file.isDirectory) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = "Directory",
            tint = Color(0xFFE5A93B),
            modifier = Modifier.size(18.dp)
        )
    } else {
        val extension = file.extension.lowercase()
        val pIconTint: Color
        val pIcon: androidx.compose.ui.graphics.vector.ImageVector

        when (extension) {
            "html", "htm" -> {
                pIcon = Icons.Default.Html
                pIconTint = Color(0xFFE44D26)
            }
            "css" -> {
                pIcon = Icons.Default.Css
                pIconTint = Color(0xFF264DE4)
            }
            "js" -> {
                pIcon = Icons.Default.Javascript
                pIconTint = Color(0xFFF7DF1E)
            }
            "py" -> {
                pIcon = Icons.Default.Terminal
                pIconTint = Color(0xFF3776AB)
            }
            "kt" -> {
                pIcon = Icons.Default.Code
                pIconTint = Color(0xFFA53BFC)
            }
            "txt" -> {
                pIcon = Icons.Default.Description
                pIconTint = Color.Gray
            }
            else -> {
                pIcon = Icons.Default.Article
                pIconTint = theme.textColor.copy(alpha = 0.6f)
            }
        }

        Icon(
            imageVector = pIcon,
            contentDescription = "File Type",
            tint = pIconTint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun TabsRow(
    theme: EditorTheme,
    tabs: List<String>,
    activePath: String?,
    onTabSelect: (String) -> Unit,
    onCloseTab: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(theme.sidebarBackground)
            .drawBehind {
                drawLine(
                    color = theme.textColor.copy(alpha = 0.08f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(tabs) { tabPath ->
            val isSelected = tabPath == activePath
            val bgTab = if (isSelected) theme.editorBackground else theme.sidebarBackground
            val textTabColor = if (isSelected) theme.keywordColor else theme.textColor.copy(alpha = 0.7f)
            val borderTabSide = if (isSelected) {
                Modifier.drawBehind {
                    drawLine(
                        color = theme.keywordColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            } else Modifier

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 180.dp)
                    .background(bgTab)
                    .then(borderTabSide)
                    .clickable { onTabSelect(tabPath) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = tabPath.substringAfterLast('/'),
                    fontSize = 12.sp,
                    color = textTabColor,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(end = 6.dp)
                )

                IconButton(
                    onClick = { onCloseTab(tabPath) },
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Tab",
                        tint = textTabColor,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
            Divider(
                color = theme.textColor.copy(alpha = 0.06f),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
            )
        }
    }
}

@Composable
fun TextAndCodeEditor(
    theme: EditorTheme,
    content: String,
    extension: String,
    fontSize: Int,
    onContentChange: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
    ) {
        // Simple editor helper bar with Undo / Redo visual trigger keys
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onUndo, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Undo, "Undo", tint = theme.textColor.copy(alpha = 0.8f))
            }
            IconButton(onClick = onRedo, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Redo, "Redo", tint = theme.textColor.copy(alpha = 0.8f))
            }
        }

        Divider(color = theme.textColor.copy(alpha = 0.04f))

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Generous editor Gutter with Line numbers
            val lineCount = content.lines().size
            val lineNumbersJoined = remember(lineCount) {
                (1..lineCount.coerceAtLeast(1)).joinToString("\n")
            }
            Column(
                modifier = Modifier
                    .padding(top = 1.dp, start = 8.dp, end = 12.dp)
                    .widthIn(min = 28.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = lineNumbersJoined,
                    color = theme.textColor.copy(alpha = 0.28f),
                    fontSize = fontSize.sp,
                    fontFamily = FontFamily.Monospace,
                    style = TextStyle(lineHeight = (fontSize * 1.45).sp)
                )
            }

            val visualTransformation = remember(extension, theme) {
                SyntaxHighlightingTransformation(extension, theme)
            }

            // Highlighting Code Input Box
            BasicTextField(
                value = content,
                onValueChange = onContentChange,
                textStyle = TextStyle(
                    color = theme.textColor,
                    fontSize = fontSize.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = (fontSize * 1.45).sp
                ),
                cursorBrush = SolidColor(theme.cursorColor),
                visualTransformation = visualTransformation,
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    imeAction = ImeAction.Default
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 16.dp, bottom = 48.dp)
            )
        }
    }
}

@Composable
fun EmptyWorkspaceView(
    theme: EditorTheme,
    onCreateFile: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.editorBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = "Empty editor logo",
                tint = theme.keywordColor.copy(alpha = 0.6f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "VStudio Workspace Active",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textColor,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Open a script from the explorer on the left or create a new file to start coding sandbox programs.",
                fontSize = 13.sp,
                color = theme.textColor.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onCreateFile,
                colors = ButtonDefaults.buttonColors(containerColor = theme.keywordColor)
            ) {
                Text("Create New File", color = Color.White)
            }
        }
    }
}

@Composable
fun TerminalConsolePanel(
    theme: EditorTheme,
    output: String?,
    isWeb: Boolean,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color(0xFF0C0C0C))
            .drawBehind {
                drawLine(
                    color = Color(0xFF333333),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isWeb) Icons.Default.Web else Icons.Default.Terminal,
                        contentDescription = "Console Type",
                        tint = Color(0xFF007ACC),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isWeb) "LIVE WEB PREVIEW SHIFT" else "COMPILER SIMULATOR TERMINAL",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Term closing",
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Output panel area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(Color(0xFF0A0A0A))
            ) {
                if (isWeb && output != null) {
                    // Live Local WebView Engine
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL("file:///android_asset/", output, "text/html", "UTF-8", null)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Script Console Logs Output
                    val scrollTermState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollTermState)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = output ?: "Empty terminal buffer. Press run button to execute script.",
                            color = Color(0xFFCCCCCC),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeveloperSymbolToolbar(
    theme: EditorTheme,
    editorText: String,
    onInsert: (String) -> Unit
) {
    val developerTools = listOf(
        "Tab", "{", "}", "(", ")", "[", "]", "\"", "'", "<", ">", ";", "=", "+", "-", "_", "/", "*", "?"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = theme.sidebarBackground),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier
            .border(1.dp, theme.textColor.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .wrapContentWidth()
    ) {
        LazyRow(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(developerTools) { sym ->
                Box(
                    modifier = Modifier
                        .background(
                            color = if (sym == "Tab") theme.keywordColor.copy(0.3f) else theme.editorBackground,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onInsert(sym) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = sym,
                        color = if (sym == "Tab") theme.keywordColor else theme.textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
