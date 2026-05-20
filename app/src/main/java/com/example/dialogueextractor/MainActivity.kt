package com.example.dialogueextractor

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private var selectedInputUri: Uri? = null
    private var selectedInputDisplayName: String? = null

    private val pickInputFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedInputUri = uri
            selectedInputDisplayName = queryDisplayName(uri)
            contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            statusText.text = "已选择输入文件: ${selectedInputDisplayName ?: uri.lastPathSegment}"
        } else {
            statusText.text = "未选择文件。"
        }
    }

    private val createOutputFile = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) {
            statusText.text = "未创建输出文件。"
            return@registerForActivityResult
        }

        val inputUri = selectedInputUri
        if (inputUri == null) {
            statusText.text = "请先选择输入文件。"
            return@registerForActivityResult
        }

        runCatching {
            val inputContent = contentResolver.openInputStream(inputUri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("无法读取输入文件")
            val outputText = extractDialogueFromJsonText(inputContent)
            contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(outputText) }
                ?: error("无法写入输出文件")
        }.onSuccess {
            statusText.text = "提取成功，已保存输出文件。"
        }.onFailure { ex ->
            statusText.text = "处理失败: ${ex.message}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val btnPick: Button = findViewById(R.id.btnPick)
        val btnExtract: Button = findViewById(R.id.btnExtract)

        btnPick.setOnClickListener {
            pickInputFile.launch(arrayOf("text/plain", "application/json", "*/*"))
        }

        btnExtract.setOnClickListener {
            val outputName = buildOutputFileName()
            createOutputFile.launch(outputName)
        }
    }

    private fun buildOutputFileName(): String {
        val inputName = selectedInputDisplayName ?: return "extracted_dialogue.txt"
        val dotIndex = inputName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) inputName.substring(0, dotIndex) else inputName
        return "${baseName}_extract.txt"
    }

    private fun queryDisplayName(uri: Uri): String? {
        return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
    }

    private fun extractDialogueFromJsonText(raw: String): String {
        val root = JSONObject(raw)
        val chunks = root.optJSONObject("chunkedPrompt")?.optJSONArray("chunks")
            ?: return "未找到 chunkedPrompt.chunks 数据。"

        val turns = mutableListOf<String>()

        for (i in 0 until chunks.length()) {
            val chunk = chunks.optJSONObject(i) ?: continue
            if (chunk.optBoolean("isThought", false) || chunk.optBoolean("thought", false)) continue

            val role = chunk.optString("role", "").trim().lowercase()
            if (role.isEmpty()) continue

            val sender = if (role == "user") "用户" else "AI"
            val textContent = extractTextContent(chunk).trim().replace("\\n", "\n")

            if (textContent.isNotBlank()) {
                turns.add("【$sender】:\n$textContent")
            }
        }

        return if (turns.isEmpty()) {
            "没有可提取的对话内容（已自动过滤思维链与空内容）。"
        } else {
            turns.joinToString("\n\n========================================\n\n")
        }
    }

    private fun extractTextContent(chunk: JSONObject): String {
        val parts = chunk.optJSONArray("parts") ?: return chunk.optString("text", "")

        val pieces = mutableListOf<String>()
        for (j in 0 until parts.length()) {
            val part = parts.optJSONObject(j) ?: continue
            if (part.optBoolean("thought", false) || part.optBoolean("isThought", false)) continue
            pieces.add(part.optString("text", ""))
        }
        return pieces.joinToString("")
    }
}
