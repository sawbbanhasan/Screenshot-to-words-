package com.example.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object DocExporter {

    /**
     * Simple parser that converts Markdown formatting into standard, beautifully styled HTML
     * designed to be opened in Microsoft Word as a formatted document.
     */
    fun markdownToHtml(markdown: String): String {
        val lines = markdown.lines()
        val htmlBuilder = StringBuilder()
        
        var inList = false
        var inTable = false
        var tableHeaders: List<String>? = null
        
        for (rawLine in lines) {
            val line = rawLine.trim()
            
            // Handle Table ends
            if (inTable && !line.startsWith("|")) {
                htmlBuilder.append("</tbody></table>\n")
                inTable = false
                tableHeaders = null
            }
            
            // Handle List ends
            if (inList && !line.startsWith("-") && !line.startsWith("*") && !line.matches(Regex("^\\d+\\.\\s+.*"))) {
                htmlBuilder.append("</ul>\n")
                inList = false
            }

            // Skip table separators (e.g., |---|---|)
            if (line.matches(Regex("^\\|?\\s*[-:]+\\s*\\|.*")) || line.matches(Regex("^\\|?\\s*[:---:]+\\s*\\|.*"))) {
                continue
            }

            when {
                // 1. Headings
                line.startsWith("# ") -> {
                    val title = parseInlineFormatting(line.substring(2))
                    htmlBuilder.append("<h1>$title</h1>\n")
                }
                line.startsWith("## ") -> {
                    val title = parseInlineFormatting(line.substring(3))
                    htmlBuilder.append("<h2>$title</h2>\n")
                }
                line.startsWith("### ") -> {
                    val title = parseInlineFormatting(line.substring(4))
                    htmlBuilder.append("<h3>$title</h3>\n")
                }
                
                // 2. Tables
                line.startsWith("|") -> {
                    val cols = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    if (!inTable) {
                        inTable = true
                        htmlBuilder.append("<table>\n<thead>\n<tr>\n")
                        for (col in cols) {
                            htmlBuilder.append("<th>${parseInlineFormatting(col)}</th>\n")
                        }
                        htmlBuilder.append("</tr>\n</thead>\n<tbody>\n")
                        tableHeaders = cols
                    } else {
                        htmlBuilder.append("<tr>\n")
                        for (col in cols) {
                            htmlBuilder.append("<td>${parseInlineFormatting(col)}</td>\n")
                        }
                        htmlBuilder.append("</tr>\n")
                    }
                }
                
                // 3. Lists (Bulleted)
                line.startsWith("- ") || line.startsWith("* ") -> {
                    val content = parseInlineFormatting(line.substring(2))
                    if (!inList) {
                        inList = true
                        htmlBuilder.append("<ul>\n")
                    }
                    htmlBuilder.append("<li>$content</li>\n")
                }
                
                // 4. Lists (Numbered)
                line.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val match = Regex("^\\d+\\.\\s+(.*)").find(line)
                    val content = parseInlineFormatting(match?.groupValues?.get(1) ?: line)
                    if (!inList) {
                        inList = true
                        htmlBuilder.append("<ol>\n")
                    }
                    htmlBuilder.append("<li>$content</li>\n")
                }
                
                // 5. Blockquotes
                line.startsWith("> ") -> {
                    val content = parseInlineFormatting(line.substring(2))
                    htmlBuilder.append("<blockquote>$content</blockquote>\n")
                }
                
                // 6. Plain Paragraphs
                line.isNotEmpty() -> {
                    val content = parseInlineFormatting(line)
                    htmlBuilder.append("<p>$content</p>\n")
                }
                
                // Empty lines
                else -> {
                    // Do nothing or close lists if empty line
                }
            }
        }
        
        // Safety close-outs
        if (inTable) {
            htmlBuilder.append("</tbody></table>\n")
        }
        if (inList) {
            htmlBuilder.append("</ul>\n")
        }
        
        return getFullWordTemplate(htmlBuilder.toString())
    }

    private fun parseInlineFormatting(text: String): String {
        var result = text
            // Escape HTML characters
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            
        // Bold: **text** or __text__
        result = result.replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
        result = result.replace(Regex("__(.*?)__"), "<b>$1</b>")
        
        // Italics: *text* or _text_
        result = result.replace(Regex("\\*(.*?)\\*"), "<i>$1</i>")
        result = result.replace(Regex("_(.*?)_"), "<i>$1</i>")
        
        // Code: `code`
        result = result.replace(Regex("`(.*?)`"), "<code style='background-color:#F4F4F4; padding:2px 4px; font-family:Consolas,monospace; font-size:0.9em; border-radius:3px;'>$1</code>")
        
        return result
    }

    private fun getFullWordTemplate(bodyContent: String): String {
        return """
            <html xmlns:o='urn:schemas-microsoft-com:office:office' 
                  xmlns:w='urn:schemas-microsoft-com:office:word' 
                  xmlns='http://www.w3.org/TR/REC-html40'>
            <head>
                <meta charset="utf-8">
                <title>Screenshot to Word Document</title>
                <!--[if gte mso 9]>
                <xml>
                    <w:WordDocument>
                        <w:View>Print</w:View>
                        <w:Zoom>100</w:Zoom>
                        <w:DoNotOptimizeForBrowser/>
                    </w:WordDocument>
                </xml>
                <![endif]-->
                <style>
                    body {
                        font-family: 'Calibri', 'Segoe UI', Arial, sans-serif;
                        font-size: 11pt;
                        line-height: 1.6;
                        color: #2b2b2b;
                        margin: 1in;
                    }
                    h1 {
                        font-family: 'Segoe UI Light', 'Calibri Light', sans-serif;
                        font-size: 24pt;
                        color: #1a365d;
                        margin-bottom: 12pt;
                        border-bottom: 2px solid #1a365d;
                        padding-bottom: 4pt;
                    }
                    h2 {
                        font-family: 'Segoe UI Semibold', sans-serif;
                        font-size: 16pt;
                        color: #2c5282;
                        margin-top: 18pt;
                        margin-bottom: 6pt;
                    }
                    h3 {
                        font-family: 'Segoe UI Semibold', sans-serif;
                        font-size: 13pt;
                        color: #4a5568;
                        margin-top: 14pt;
                        margin-bottom: 4pt;
                    }
                    p {
                        margin-bottom: 10pt;
                        text-align: justify;
                    }
                    table {
                        border-collapse: collapse;
                        width: 100%;
                        margin-top: 12pt;
                        margin-bottom: 12pt;
                    }
                    th {
                        background-color: #ebf8ff;
                        border: 1px solid #cbd5e0;
                        color: #2b6cb0;
                        font-weight: bold;
                        padding: 8pt;
                        text-align: left;
                    }
                    td {
                        border: 1px solid #cbd5e0;
                        padding: 8pt;
                        text-align: left;
                    }
                    ul, ol {
                        margin-top: 0;
                        margin-bottom: 10pt;
                        padding-left: 20pt;
                    }
                    li {
                        margin-bottom: 4pt;
                    }
                    blockquote {
                        margin: 12pt 0;
                        padding: 6pt 12pt;
                        border-left: 4pt solid #cbd5e0;
                        background-color: #f7fafc;
                        color: #4a5568;
                        font-style: italic;
                    }
                </style>
            </head>
            <body>
                $bodyContent
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Saves the converted content to a temporary .doc file in cache and returns its shareable URI.
     */
    fun exportToWordFile(context: Context, filename: String, markdown: String): Uri? {
        return try {
            val htmlContent = markdownToHtml(markdown)
            val cacheDir = File(context.cacheDir, "documents").apply { mkdirs() }
            val cleanName = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val docFile = File(cacheDir, if (cleanName.endsWith(".doc")) cleanName else "$cleanName.doc")
            
            FileOutputStream(docFile).use { fos ->
                fos.write(htmlContent.toByteArray(Charsets.UTF_8))
            }
            
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                docFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves the converted content to a temporary .txt file in cache and returns its shareable URI.
     */
    fun exportToTextFile(context: Context, filename: String, content: String): Uri? {
        return try {
            val cacheDir = File(context.cacheDir, "documents").apply { mkdirs() }
            val cleanName = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val txtFile = File(cacheDir, if (cleanName.endsWith(".txt")) cleanName else "$cleanName.txt")
            
            FileOutputStream(txtFile).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
            }
            
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                txtFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
