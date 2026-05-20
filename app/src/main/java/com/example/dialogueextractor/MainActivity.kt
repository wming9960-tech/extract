package com.example.dialogueextractor

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.dialogueextractor.databinding.ActivityMainBinding
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var extractedText: String? = null

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            handleInputFile(uri)
        }
    }

    private val saveFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            saveOutput(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPickFile.setOnClickListener {
            pickFileLauncher.launch(arrayOf("text/plain", "application/json", "*/*"))
        }

        binding.btnSaveFile.setOnClickListener {
            saveFileLauncher.launch("extracted_dialogue.txt")
        }
    }

    private fun handleInputFile(uri: Uri) {
        runCatching {
            val content = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("无法读取文件")
            val json = JSONObject(content)
            extractDialogue(json)
        }.onSuccess { result ->
            extractedText = result
            binding.btnSaveFile.isEnabled = true
            binding.tvStatus.text = "状态：解析完成，可导出 TXT。"
        }.onFailure { err ->
            extractedText = null
            binding.btnSaveFile.isEnabled = false
            binding.tvStatus.text = "状态：解析失败 - ${err.message}"
            Toast.makeText(this, "解析失败，请确认 TXT 内容是有效 JSON", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveOutput(uri: Uri) {
        val text = extractedText ?: return
        runCatching {
            contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { out ->
                out.write(text)
            } ?: error("无法写入输出文件")
        }.onSuccess {
            binding.tvStatus.text = "状态：导出成功。"
            Toast.makeText(this, "导出完成", Toast.LENGTH_SHORT).show()
        }.onFailure { err ->
            binding.tvStatus.text = "状态：导出失败 - ${err.message}"
        }
    }

    private fun extractDialogue(root: JSONObject): String {
        val chunks = root.optJSONObject("chunkedPrompt")?.optJSONArray("chunks")
            ?: return ""

        val turns = mutableListOf<String>()

        for (i in 0 until chunks.length()) {
            val chunk = chunks.optJSONObject(i) ?: continue
            if (chunk.optBoolean("isThought") || chunk.optBoolean("thought")) continue

            val role = chunk.optString("role").lowercase()
            if (role.isBlank()) continue
            val sender = if (role == "user") "用户" else "AI"

            val text = buildString {
                val parts = chunk.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    for (p in 0 until parts.length()) {
                        val part = parts.optJSONObject(p) ?: continue
                        if (part.optBoolean("isThought") || part.optBoolean("thought")) continue
                        append(part.optString("text"))
                    }
                } else {
                    append(chunk.optString("text"))
                }
            }.replace("\\n", "\n").trim()

            if (text.isNotBlank()) {
                turns.add("【$sender】:\n$text")
            }
        }

        return turns.joinToString("\n\n========================================\n\n")
    }
}
