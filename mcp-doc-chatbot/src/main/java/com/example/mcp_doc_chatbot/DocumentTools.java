package com.example.mcp_doc_chatbot;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ┌─────────────────────────────────────────────────────────────┐
 * │                    MCP Tools Layer                          │
 * │                                                             │
 * │  These methods are the "MCP Tools" — capabilities exposed   │
 * │  to the LLM. Spring AI serializes their signatures into a  │
 * │  JSON tool schema, which gets sent to the model alongside   │
 * │  the conversation. The model decides WHEN to call them.     │
 * │                                                             │
 * │  In a real MCP setup, these would live in an MCP Server    │
 * │  process. Spring AI abstracts that with @Tool — same idea. │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Tools provided:
 *   1. listDocuments  — browse available docs
 *   2. readDocument   — read a doc's full content
 *   3. editDocument   — overwrite a doc's content
 *   4. createDocument — create a brand new doc
 */
@Component
 public class DocumentTools {

    @Value("${docs.directory:./docs}")
    private String docsDirectory;

    // ─── Tool 1: List ────────────────────────────────────────────

    /**
     * Lists all .txt / .md files in the docs folder.
     * The LLM will call this when the user asks "what docs do I have?"
     * or when it needs to know available files before reading one.
     */
    @Tool(description = """
            List all documents available in the docs directory.
            Returns a newline-separated list of filenames.
            Call this to discover what documents exist before trying to read one.
            """)
    public String listDocuments() {
        Path dir = Path.of(docsDirectory);
        try (Stream<Path> paths = Files.list(dir)) {
            String files = paths
                    .filter(p -> !Files.isDirectory(p))
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.endsWith(".txt") || name.endsWith(".md");
                    })
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.joining("\n"));

            return files.isEmpty()
                    ? "No documents found in the docs directory."
                    : "Available documents:\n" + files;

        } catch (IOException e) {
            return "Error listing documents: " + e.getMessage();
        }
    }

    // ─── Tool 2: Read ────────────────────────────────────────────

    /**
     * Reads a document by name. The LLM calls this when the user refers
     * to a document and it needs to fetch the latest content (e.g. after
     * an edit, or when it wasn't auto-injected via @mention).
     *
     * @param documentName  filename, e.g. "notes.md" or "todo.txt"
     */
    @Tool(description = """
            Read the full contents of a document by its filename.
            Use this to retrieve a document's text so you can answer questions about it.
            documentName must be just the filename (e.g. "notes.md"), not a full path.
            """)
    public String readDocument(String documentName) {
        Path path = resolveDoc(documentName);
        if (!Files.exists(path)) {
            return "Document '" + documentName + "' not found. Use listDocuments() to see available files.";
        }
        try {
            String content = Files.readString(path);
            return "=== " + documentName + " ===\n" + content;
        } catch (IOException e) {
            return "Error reading '" + documentName + "': " + e.getMessage();
        }
    }

    // ─── Tool 3: Edit ────────────────────────────────────────────

    /**
     * Overwrites a document with new content.
     * The LLM calls this when the user explicitly asks to edit / update / rewrite a doc.
     *
     * @param documentName  filename of the doc to edit
     * @param newContent    the full new content to write (not a diff — full replacement)
     */
    @Tool(description = """
            Overwrite the content of an existing document.
            Use this when the user asks to edit, update, rewrite, or append to a document.
            newContent should be the COMPLETE new content for the file (full replacement, not a diff).
            documentName must be just the filename (e.g. "notes.md").
            """)
    public String editDocument(String documentName, String newContent) {
        Path path = resolveDoc(documentName);
        if (!Files.exists(path)) {
            return "Document '" + documentName + "' not found. Create it first with createDocument().";
        }
        try {
            Files.writeString(path, newContent, StandardOpenOption.TRUNCATE_EXISTING);
            return "✓ Successfully updated '" + documentName + "' (" + newContent.length() + " chars).";
        } catch (IOException e) {
            return "Error writing '" + documentName + "': " + e.getMessage();
        }
    }

    // ─── Tool 4: Create ──────────────────────────────────────────

    /**
     * Creates a new document. Called when the user asks to create a new file.
     *
     * @param documentName  name for the new file (must end in .txt or .md)
     * @param initialContent initial text to write into it
     */
    @Tool(description = """
            Create a brand new document with a given filename and initial content.
            documentName must end in .txt or .md (e.g. "ideas.md").
            Returns an error if the file already exists (use editDocument to modify existing files).
            """)
    public String createDocument(String documentName, String initialContent) {
        if (!documentName.endsWith(".txt") && !documentName.endsWith(".md")) {
            return "Error: documentName must end in .txt or .md";
        }
        Path path = resolveDoc(documentName);
        if (Files.exists(path)) {
            return "Document '" + documentName + "' already exists. Use editDocument() to modify it.";
        }
        try {
            Files.writeString(path, initialContent, StandardOpenOption.CREATE_NEW);
            return "✓ Created new document '" + documentName + "'.";
        } catch (IOException e) {
            return "Error creating '" + documentName + "': " + e.getMessage();
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private Path resolveDoc(String name) {
        // Security: strip any path separators to prevent directory traversal
        String safeName = Path.of(name).getFileName().toString();
        return Path.of(docsDirectory).resolve(safeName);
    }
}