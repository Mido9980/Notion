package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ProjectRepository(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "vstudio_code_db"
    ).build()

    private val fileDao = database.fileDao()

    val allFiles: Flow<List<ProjectFile>> = fileDao.getAllFiles()

    suspend fun getFile(path: String): ProjectFile? = fileDao.getFileByPath(path)

    suspend fun insertFile(file: ProjectFile) = fileDao.insertFile(file)

    suspend fun deleteFile(path: String) {
        val file = fileDao.getFileByPath(path)
        if (file != null) {
            if (file.isDirectory) {
                // Remove directory and all children
                fileDao.deleteFilesWithPrefix(path + "/")
                fileDao.deleteFileByPath(path)
            } else {
                fileDao.deleteFileByPath(path)
            }
        }
    }

    suspend fun createFile(path: String, isDirectory: Boolean) {
        val content = if (isDirectory) "" else getSeedContentForPath(path)
        val file = ProjectFile(path, isDirectory, content)
        fileDao.insertFile(file)
    }

    suspend fun renameFile(oldPath: String, newPath: String) {
        val file = fileDao.getFileByPath(oldPath) ?: return
        if (file.isDirectory) {
            val all = fileDao.getAllFiles().first()
            for (f in all) {
                if (f.path.startsWith("$oldPath/")) {
                    val newChildPath = f.path.replaceFirst("$oldPath/", "$newPath/")
                    fileDao.insertFile(f.copy(path = newChildPath))
                    fileDao.deleteFileByPath(f.path)
                }
            }
            fileDao.insertFile(file.copy(path = newPath))
            fileDao.deleteFileByPath(oldPath)
        } else {
            fileDao.insertFile(file.copy(path = newPath))
            fileDao.deleteFileByPath(oldPath)
        }
    }

    suspend fun prepopulateIfEmpty() {
        val files = fileDao.getAllFiles().first()
        if (files.isEmpty()) {
            val defaults = listOf(
                ProjectFile("index.html", false, getIndexHtmlContent()),
                ProjectFile("style.css", false, getStyleCssContent()),
                ProjectFile("script.js", false, getScriptJsContent()),
                ProjectFile("HelloWorld.kt", false, getKotlinContent()),
                ProjectFile("Calculator.py", false, getPythonContent()),
                ProjectFile("assets", true),
                ProjectFile("assets/readme.txt", false, getReadmeContent())
            )
            for (f in defaults) {
                fileDao.insertFile(f)
            }
        }
    }

    private fun getSeedContentForPath(path: String): String {
        return when (path.substringAfterLast('.', "").lowercase()) {
            "html" -> "<!DOCTYPE html>\n<html>\n<head>\n  <title>New File</title>\n</head>\n<body>\n  <h1>Hello World</h1>\n</body>\n</html>"
            "css" -> "body {\n  background-color: #121212;\n  color: #ffffff;\n  font-family: sans-serif;\n}"
            "js" -> "console.log(\"Hello from Script!\");"
            "py" -> "print(\"Hello, Python world!\")"
            "kt" -> "fun main() {\n    println(\"Hello, Kotlin world!\")\n}"
            else -> ""
        }
    }

    private fun getIndexHtmlContent() = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Responsive Counter App</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="card">
        <h1>VStudio Live Web Preview</h1>
        <p class="subtitle">This is running in live reactive web simulator state</p>
        
        <div class="counter-box">
            <span id="counter">0</span>
        </div>

        <div class="btn-group">
            <button id="btn-dec" class="btn btn-secondary">- Decrease</button>
            <button id="btn-inc" class="btn btn-primary">+ Increase</button>
        </div>

        <div class="theme-box">
            <button id="btn-theme" class="btn btn-outline">Toggle Hue Rotation</button>
        </div>
    </div>
    <script src="script.js"></script>
</body>
</html>
    """.trim()

    private fun getStyleCssContent() = """
/* VS Code Dark Neon Aesthetics */
body {
    background-color: #0e1117;
    color: #f0f6fc;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 90vh;
    margin: 0;
    transition: filter 0.5s ease;
}

.card {
    background: #161b22;
    border: 1px solid #30363d;
    border-radius: 12px;
    padding: 30px;
    text-align: center;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
    max-width: 400px;
    width: 90%;
}

h1 {
    color: #58a6ff;
    font-size: 24px;
    margin-bottom: 8px;
}

.subtitle {
    color: #8b949e;
    font-size: 14px;
    margin-bottom: 24px;
}

.counter-box {
    font-size: 64px;
    font-weight: bold;
    color: #ff7b72;
    margin: 20px 0;
    text-shadow: 0 0 10px rgba(255, 123, 114, 0.3);
}

.btn-group {
    display: flex;
    gap: 12px;
    justify-content: center;
    margin-bottom: 20px;
}

.btn {
    padding: 10px 20px;
    font-size: 15px;
    font-weight: 600;
    border-radius: 6px;
    border: none;
    cursor: pointer;
    transition: transform 0.1s, opacity 0.2s;
}

.btn:active {
    transform: scale(0.96);
}

.btn-primary {
    background-color: #238636;
    color: #ffffff;
}

.btn-secondary {
    background-color: #21262d;
    color: #c9d1d9;
    border: 1px solid #30363d;
}

.btn-outline {
    background: transparent;
    color: #58a6ff;
    border: 1px solid #58a6ff;
    width: 100%;
}

.btn-primary:hover { opacity: 0.9; }
.btn-secondary:hover { background-color: #30363d; }
    """.trim()

    private fun getScriptJsContent() = """
// Interactive logic simulation
let count = 0;
const counterEl = document.getElementById("counter");
const btnInc = document.getElementById("btn-inc");
const btnDec = document.getElementById("btn-dec");
const btnTheme = document.getElementById("btn-theme");

btnInc.addEventListener("click", () => {
    count++;
    counterEl.textContent = count;
});

btnDec.addEventListener("click", () => {
    count--;
    counterEl.textContent = count;
});

let rotated = false;
btnTheme.addEventListener("click", () => {
    rotated = !rotated;
    document.body.style.filter = rotated ? "hue-rotate(90deg)" : "none";
});
    """.trim()

    private fun getKotlinContent() = """
fun main() {
    println("--- KOTLIN COMPILER SIMULATOR ---")
    val language = "Kotlin"
    println("Initializing execution for ${"$"}{language} code...")
    
    // Calculate first 10 Fibonacci numbers
    var t1 = 0
    var t2 = 1
    print("Fibonacci Series (10 terms): ")
    for (i in 1..10) {
        print("${"$"}{t1} ")
        val sum = t1 + t2
        t1 = t2
        t2 = sum
    }
    println("\n\nExecution finished successfully (exit code 0).")
}
    """.trim()

    private fun getPythonContent() = """
# Interactive Python Calculator Simulation
def calculate_grades():
    print("--- PYTHON RUNTIME SANDBOX ---")
    students = ["Alex", "Elena", "Marcus", "Clara"]
    scores = [85, 92, 78, 95]
    
    total = sum(scores)
    average = total / len(scores)
    
    print(f"Loaded student database: {students}")
    print(f"Calculating grade statistics...")
    print(f"Average Class Score: {average:.2f}%")
    
    print("\nRanking High Performers:")
    for name, score in zip(students, scores):
        status = "Pass" if score >= 80 else "Needs Improvement"
        print(f" - {name}: {score} ({status})")

calculate_grades()
    """.trim()

    private fun getReadmeContent() = """
Welcome to VStudio Code Sandbox!

Explore Features:
1. File Explorer on the left drawer. Create, rename, or delete files/folders.
2. Multiple tabs to edit files in parallel.
3. Live Web Preview: If you edit index.html, style.css, or script.js, run "Live Preview" to render it on a responsive canvas.
4. Python and Kotlin compiler simulator: Run to verify functional output consoles!
5. Rich Syntax Highlighting & Code Themes (Dark Modern, Monokai, Dracula).
    """.trim()
}
