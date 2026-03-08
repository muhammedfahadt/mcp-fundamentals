package com.example.mcp_doc_chatbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * ┌─────────────────────────────────────────────────────────────┐
 * │              @mention → Context Injection                   │
 * │                                                             │
 * │  This is the "automatic context" feature.                  │
 * │                                                             │
 * │  When a user writes:                                        │
 * │    "Summarise @meeting-notes.md"                            │
 * │                                                             │
 * │  This class intercepts the message BEFORE it goes to the   │
 * │  LLM, extracts all @mentions, reads those files, and       │
 * │  appends their contents to the message as inline context:  │
 * │                                             
 *                 │
 * │    "Summarise @meeting-notes.md                            │
 * │     ── Context: meeting-notes.md ──                        │
 * │     ...full file content..."                               │
 * │                                                             │
 * │  This is analogous to how Cursor / Claude Code inject      │
 * │  file content via MCP Resources when you @-tag a file.     │
 * └─────────────────────────────────────────────────────────────┘
 */
@Component
 public class DocumentContextInjector {

    // Matches @filename.txt  or  @filename.md  (word chars, hyphens, dots)
    private static final Pattern MENTION_PATTERN =
            Pattern.compile("@([\\w\\-]+\\.(?:txt|md))");

    @Value("${docs.directory:./docs}")
    private String docsDirectory;

    /**
     * Enriches a raw user message with the contents of any @mentioned documents.
     *
     * @param userMessage  the raw input from the user
     * @return  the message augmented with doc content blocks, ready for the LLM
     */
    public EnrichedMessage enrich(String userMessage) {
        Matcher matcher = MENTION_PATTERN.matcher(userMessage);

        List<String> mentionedDocs = new ArrayList<>();
        StringBuilder contextBlocks = new StringBuilder();

        Set<String> seen = new LinkedHashSet<>();

        while (matcher.find()) {
            String docName = matcher.group(1);
            if (seen.add(docName)) {  // deduplicate multiple mentions of same doc
                mentionedDocs.add(docName);
                String content = readDoc(docName);
                contextBlocks
                        .append("\n\n── Context: ").append(docName).append(" ──\n")
                        .append(content)
                        .append("\n── End of ").append(docName).append(" ──");
            }
        }

        if (contextBlocks.isEmpty()) {
            // No @mentions — return original message unchanged
            return new EnrichedMessage(userMessage, Collections.emptyList());
        }

        // Append context AFTER the user's message so the original intent stays upfront
        String enriched = userMessage + contextBlocks;
        return new EnrichedMessage(enriched, mentionedDocs);
    }

    private String readDoc(String docName) {
        // Strip path traversal
        String safeName = Path.of(docName).getFileName().toString();
        Path path = Path.of(docsDirectory).resolve(safeName);

        if (!Files.exists(path)) {
            return "[Document not found: " + docName + "]";
        }
        try {
            return Files.readString(path);
        } catch (IOException e) {
            return "[Error reading " + docName + ": " + e.getMessage() + "]";
        }
    }

    // ─── Value object ─────────────────────────────────────────────

    /**
     * Holds the enriched message and the list of docs that were injected,
     * so the CLI can tell the user which files were auto-loaded.
     */
    public record EnrichedMessage(
            String content,
            List<String> injectedDocs
    ) {}
}