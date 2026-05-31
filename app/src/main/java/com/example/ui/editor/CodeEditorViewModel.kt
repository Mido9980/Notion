package com.example.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProjectFile
import com.example.data.ProjectRepository
import com.example.ui.theme.EditorTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CodeEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProjectRepository(application)

    val files: StateFlow<List<ProjectFile>> = repository.allFiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _openedTabs = MutableStateFlow<List<String>>(emptyList())
    val openedTabs: StateFlow<List<String>> = _openedTabs.asStateFlow()

    private val _activeTabPath = MutableStateFlow<String?>(null)
    val activeTabPath: StateFlow<String?> = _activeTabPath.asStateFlow()

    private val _editorContent = MutableStateFlow("")
    val editorContent: StateFlow<String> = _editorContent.asStateFlow()

    private val _isExplorerOpen = MutableStateFlow(true)
    val isExplorerOpen: StateFlow<Boolean> = _isExplorerOpen.asStateFlow()

    private val _selectedTheme = MutableStateFlow(EditorTheme.DarkModern)
    val selectedTheme: StateFlow<EditorTheme> = _selectedTheme.asStateFlow()

    private val _fontSize = MutableStateFlow(14)
    val fontSize: StateFlow<Int> = _fontSize.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _replaceQuery = MutableStateFlow("")
    val replaceQuery: StateFlow<String> = _replaceQuery.asStateFlow()

    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    private val _terminalOutput = MutableStateFlow<String?>(null)
    val terminalOutput: StateFlow<String?> = _terminalOutput.asStateFlow()

    private val _isTerminalOpen = MutableStateFlow(false)
    val isTerminalOpen: StateFlow<Boolean> = _isTerminalOpen.asStateFlow()

    private val _isWebPreview = MutableStateFlow(false)
    val isWebPreview: StateFlow<Boolean> = _isWebPreview.asStateFlow()

    // Undo/Redo Stacks
    private val undoStack = java.util.ArrayDeque<String>()
    private val redoStack = java.util.ArrayDeque<String>()
    private var isUndoRedoAction = false
    private var saveJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
            
            // Collect the first elements from flow to select active files
            repository.allFiles.collect { list ->
                if (list.isNotEmpty() && _activeTabPath.value == null) {
                    val defaultFile = list.find { it.path == "assets/readme.txt" } ?: list.firstOrNull { !it.isDirectory }
                    defaultFile?.let {
                        openFile(it.path)
                    }
                }
            }
        }
    }

    fun openFile(path: String) {
        viewModelScope.launch {
            val file = repository.getFile(path) ?: return@launch
            if (file.isDirectory) return@launch

            // Save active tab content first to prevent losing edits
            _activeTabPath.value?.let { currentPath ->
                repository.getFile(currentPath)?.let { curFile ->
                    repository.insertFile(curFile.copy(content = _editorContent.value))
                }
            }

            val currentTabs = _openedTabs.value.toMutableList()
            if (!currentTabs.contains(path)) {
                currentTabs.add(path)
                _openedTabs.value = currentTabs
            }
            _activeTabPath.value = path
            
            // Clear undo/redo stacks for safety across file shifts
            undoStack.clear()
            redoStack.clear()
            
            _editorContent.value = file.content
        }
    }

    fun closeTab(path: String) {
        viewModelScope.launch {
            if (_activeTabPath.value == path) {
                repository.getFile(path)?.let {
                    repository.insertFile(it.copy(content = _editorContent.value))
                }
            }

            val currentTabs = _openedTabs.value.toMutableList()
            currentTabs.remove(path)
            _openedTabs.value = currentTabs

            if (_activeTabPath.value == path) {
                if (currentTabs.isNotEmpty()) {
                    openFile(currentTabs.last())
                } else {
                    _activeTabPath.value = null
                    _editorContent.value = ""
                }
            }
        }
    }

    fun updateContent(newContent: String) {
        val oldContent = _editorContent.value
        if (oldContent != newContent) {
            if (!isUndoRedoAction) {
                if (undoStack.size > 50) {
                    undoStack.removeFirst()
                }
                undoStack.push(oldContent)
                redoStack.clear()
            }
            _editorContent.value = newContent
            
            // Persist content updates dynamically in database, debouncing saves
            val activePath = _activeTabPath.value ?: return
            saveJob?.cancel()
            saveJob = viewModelScope.launch {
                kotlinx.coroutines.delay(600)
                repository.getFile(activePath)?.let { file ->
                    repository.insertFile(file.copy(content = newContent))
                }
            }
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            isUndoRedoAction = true
            val current = _editorContent.value
            redoStack.push(current)
            val previous = undoStack.pop()
            _editorContent.value = previous
            isUndoRedoAction = false
            autoSaveActive()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            isUndoRedoAction = true
            val current = _editorContent.value
            undoStack.push(current)
            val next = redoStack.pop()
            _editorContent.value = next
            isUndoRedoAction = false
            autoSaveActive()
        }
    }

    private fun autoSaveActive() {
        val activePath = _activeTabPath.value ?: return
        viewModelScope.launch {
            repository.getFile(activePath)?.let {
                repository.insertFile(it.copy(content = _editorContent.value))
            }
        }
    }

    fun createFile(name: String, isDirectory: Boolean) {
        viewModelScope.launch {
            val fullPath = name.trim().removePrefix("/").removeSuffix("/")
            if (fullPath.isEmpty()) return@launch
            repository.createFile(fullPath, isDirectory)
            if (!isDirectory) {
                openFile(fullPath)
            }
        }
    }

    fun deleteFile(path: String) {
        viewModelScope.launch {
            closeTab(path)
            repository.deleteFile(path)
        }
    }

    fun renameFile(oldPath: String, newName: String) {
        viewModelScope.launch {
            val parent = if (oldPath.contains('/')) oldPath.substringBeforeLast('/') + "/" else ""
            val newPath = parent + newName.trim()
            repository.renameFile(oldPath, newPath)
            
            val currentTabs = _openedTabs.value.toMutableList()
            if (currentTabs.contains(oldPath)) {
                val idx = currentTabs.indexOf(oldPath)
                currentTabs[idx] = newPath
                _openedTabs.value = currentTabs
            }
            if (_activeTabPath.value == oldPath) {
                _activeTabPath.value = newPath
            }
        }
    }

    fun toggleExplorer() {
        _isExplorerOpen.value = !_isExplorerOpen.value
    }

    fun selectTheme(theme: EditorTheme) {
        _selectedTheme.value = theme
    }

    fun adjustFontSize(increment: Boolean) {
        val currentSize = _fontSize.value
        if (increment && currentSize < 30) {
            _fontSize.value = currentSize + 1
        } else if (!increment && currentSize > 10) {
            _fontSize.value = currentSize - 1
        }
    }

    fun toggleSearchPanel() {
        _isSearchVisible.value = !_isSearchVisible.value
    }

    fun updateSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun updateReplaceQuery(r: String) {
        _replaceQuery.value = r
    }

    fun performReplaceAll() {
        val q = _searchQuery.value
        val r = _replaceQuery.value
        if (q.isNotEmpty()) {
            val updated = _editorContent.value.replace(q, r)
            updateContent(updated)
        }
    }

    fun formatActiveCode() {
        val currentText = _editorContent.value
        val activePath = _activeTabPath.value ?: return
        
        val lines = currentText.lines()
        val formatted = StringBuilder()
        var indentLevel = 0
        val indentSpacing = "    " // 4 spaces

        for (line in lines) {
            val trimmedLine = line.trim()
            
            if (trimmedLine.isEmpty()) {
                formatted.append("\n")
                continue
            }

            // Detect matching decreases
            if (trimmedLine.startsWith("}") || trimmedLine.startsWith("</") || trimmedLine.startsWith("]")) {
                indentLevel = (indentLevel - 1).coerceAtLeast(0)
            }

            // Append spaces for indentation
            repeat(indentLevel) {
                formatted.append(indentSpacing)
            }
            formatted.append(trimmedLine).append("\n")

            // Detect increases
            if (trimmedLine.endsWith("{") || trimmedLine.endsWith("<div") || trimmedLine.endsWith("<body") || trimmedLine.endsWith("<html") || (trimmedLine.startsWith("<") && !trimmedLine.endsWith(">") && !trimmedLine.contains("</")) || trimmedLine.endsWith("[")) {
                indentLevel++
            }
        }
        
        val resultText = formatted.toString().trimEnd()
        updateContent(resultText)
    }

    fun runActiveCode() {
        val activePath = _activeTabPath.value ?: return
        viewModelScope.launch {
            // Save current code
            repository.getFile(activePath)?.let {
                repository.insertFile(it.copy(content = _editorContent.value))
            }
            val list = files.value
            val activeFile = list.find { it.path == activePath } ?: return@launch

            _isTerminalOpen.value = true
            
            if (activeFile.extension.lowercase() in listOf("html", "htm", "css", "js")) {
                val targetHtmlPath = if (activeFile.extension.lowercase() == "html") activeFile.path else "index.html"
                val bundledHtml = CodeSandboxRunner.bundleWebProject(list, targetHtmlPath)
                _isWebPreview.value = true
                _terminalOutput.value = bundledHtml
            } else {
                _isWebPreview.value = false
                val stdout = CodeSandboxRunner.executeBackendSimulation(activeFile)
                _terminalOutput.value = stdout
            }
        }
    }

    fun closeTerminal() {
        _isTerminalOpen.value = false
        _terminalOutput.value = null
        _isWebPreview.value = false
    }
}
