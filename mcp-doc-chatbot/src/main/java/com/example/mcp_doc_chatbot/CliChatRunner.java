package com.example.mcp_doc_chatbot;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Scanner;

import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * ┌─────────────────────────────────────────────────────────────┐
 * │                   CLI Chat Loop                             │
 * │                                                             │
 * │  Implements CommandLineRunner so Spring Boot calls run()   │
 * │  automatically after context startup.                      │
 * │                                                             │
 * │  Flow per message:                                          │
 * │    1. Read user input                                       │
 * │    2. Handle slash commands (/help, /docs, /clear, /exit)  │
 * │    3. DocumentContextInjector scans for @mentions          │
 * │       └─ If found → file content injected into message     │
 * │    4. ChatClient sends to LLM (with memory + tools)        │
 * │    5. LLM reasons; may call readDocument / editDocument    │
 * │    6. Spring AI executes tool calls, feeds results back    │
 * │    7. Final response printed to terminal                    │
 * └─────────────────────────────────────────────────────────────┘
 */
@Component
 public class CliChatRunner implements CommandLineRunner {

    private final ChatClient chatClient;
    private final DocumentContextInjector contextInjector;

    @Value("${docs.directory:./docs}")
    private String docsDirectory;

    // ─── Construction ─────────────────────────────────────────────

    public CliChatRunner(
            ChatClient.Builder chatClientBuilder,
            DocumentTools documentTools,
            DocumentContextInjector contextInjector
    ) {
        this.contextInjector = contextInjector;

        /*
         * Build the ChatClient with:
         *  - System prompt: persona + instructions
         *  - Memory advisor: keeps conversation history across turns
         *  - Tools: the MCP-style document tools the LLM can invoke
         */
        InMemoryChatMemory memory = new InMemoryChatMemory();

        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(new MessageChatMemoryAdvisor(memory))
                .defaultTools(documentTools)   // ← registers all @Tool methods
                .build();
    }

    // ─── Main loop ────────────────────────────────────────────────

    @Override
    public void run(String... args) throws Exception {
        AnsiConsole.systemInstall();
        ensureDocsDirectory();
        seedSampleDocs();
        printWelcome();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print(ansi().fg(CYAN).a("you> ").reset());
            if (!scanner.hasNextLine()) break;

            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            // ── Slash commands ──────────────────────────────────
            if (input.startsWith("/")) {
                handleCommand(input);
                continue;
            }

            // ── @mention context injection ───────────────────────
            DocumentContextInjector.EnrichedMessage enriched = contextInjector.enrich(input);

            if (!enriched.injectedDocs().isEmpty()) {
                printInfo("Auto-loaded context from: " + String.join(", ", enriched.injectedDocs()));
            }

            // ── Send to LLM (tools available for it to call) ─────
            try {
                String response = chatClient
                        .prompt()
                        .user(enriched.content())   // enriched = original msg + doc context
                        .call()
                        .content();

                printAssistant(response);

            } catch (Exception e) {
                printError("LLM error: " + e.getMessage());
            }
        }

        AnsiConsole.systemUninstall();
    }

    // ─── Slash command handler ────────────────────────────────────

    private void handleCommand(String input) throws IOException {
        switch (input.toLowerCase()) {

            case "/help" -> printHelp();

            case "/docs" -> {
                // Quick local listing (doesn't go through the LLM)
                try (var paths = Files.list(Path.of(docsDirectory))) {
                    List<String> files = paths
                            .filter(p -> !Files.isDirectory(p))
                            .map(p -> p.getFileName().toString())
                            .filter(n -> n.endsWith(".txt") || n.endsWith(".md"))
                            .sorted()
                            .toList();
                    if (files.isEmpty()) {
                        printInfo("No documents found in " + docsDirectory);
                    } else {
                        printInfo("Documents in " + docsDirectory + ":");
                        files.forEach(f -> System.out.println(
                                ansi().fg(GREEN).a("  • ").reset().a(f)));
                    }
                }
            }

            case "/clear" -> {
                // ANSI clear screen
                System.out.print(ansi().eraseScreen().cursor(1, 1));
                printWelcome();
            }

            case "/exit", "/quit" -> {
                printInfo("Goodbye!");
                System.exit(0);
            }

            default -> printError("Unknown command: " + input + "  (try /help)");
        }
    }

    // ─── UI helpers ───────────────────────────────────────────────

    private void printWelcome() {
        System.out.println(ansi().fg(YELLOW).bold()
                .a("╔══════════════════════════════════════════════╗").reset());
        System.out.println(ansi().fg(YELLOW).bold()
                .a("║        MCP Document Chatbot  📄               ║").reset());
        System.out.println(ansi().fg(YELLOW).bold()
                .a("╚══════════════════════════════════════════════╝").reset());
        System.out.println(ansi().fg(WHITE)
                .a("  Chat with your documents. The LLM can read & edit them.")
                .reset());
        System.out.println(ansi().fg(WHITE)
                .a("  Mention a file with @filename.md to auto-load its context.")
                .reset());
        System.out.println(ansi().fg(WHITE)
                .a("  Type /help for commands.\n")
                .reset());
    }

    private void printHelp() {
        System.out.println(ansi().fg(YELLOW).a("\nCommands:").reset());
        System.out.println("  /docs    List available documents");
        System.out.println("  /clear   Clear the terminal");
        System.out.println("  /exit    Quit the chatbot");
        System.out.println(ansi().fg(YELLOW).a("\n@mention syntax:").reset());
        System.out.println("  @filename.md or @filename.txt");
        System.out.println("  The document's content is automatically injected as context.");
        System.out.println(ansi().fg(YELLOW).a("\nExample prompts:").reset());
        System.out.println("  Summarise @meeting-notes.md");
        System.out.println("  What tasks are in @todo.txt? Which are done?");
        System.out.println("  Edit @project-notes.md — add a section about deployment");
        System.out.println("  Create a new document called ideas.md with some AI project ideas\n");
    }

    private void printAssistant(String msg) {
        System.out.println(ansi().fg(GREEN).a("assistant> ").reset() + msg + "\n");
    }

    private void printInfo(String msg) {
        System.out.println(ansi().fg(MAGENTA).a("[info] ").reset() + msg);
    }

    private void printError(String msg) {
        System.out.println(ansi().fg(RED).a("[error] ").reset() + msg);
    }

    // ─── Docs setup ───────────────────────────────────────────────

    private void ensureDocsDirectory() throws IOException {
        Path dir = Path.of(docsDirectory);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            printInfo("Created docs directory at: " + dir.toAbsolutePath());
        }
    }

    /**
     * Seeds sample documents on first run so you have something to play with immediately.
     * Won't overwrite files that already exist.
     */
    private void seedSampleDocs() throws IOException {
        writeIfAbsent("meeting-notes.md", """
                # Meeting Notes — Sprint Planning
                **Date:** 2025-11-04
                **Attendees:** Alice, Bob, Carol

                ## Decisions
                - Launch v2 by end of November
                - Alice owns backend, Bob owns frontend, Carol owns QA
                - Daily standup at 9am

                ## Action Items
                - [ ] Alice: finalize API schema by Friday
                - [ ] Bob: set up Vite + Tailwind scaffold
                - [ ] Carol: write smoke test suite
                - [ ] All: review competitor pricing doc before next meeting

                ## Risks
                - Third-party payment API still in beta — monitor closely
                """);

        writeIfAbsent("todo.txt", """
                TODO List
                =========
                [x] Set up project repo
                [x] Configure CI/CD pipeline
                [ ] Write unit tests for auth module
                [ ] Set up staging environment
                [ ] Write API documentation
                [ ] Performance testing
                [ ] Security audit
                [ ] Deploy to production
                """);

        writeIfAbsent("project-notes.md", """
                # Project Alpha — Tech Notes

                ## Architecture
                - Backend: Spring Boot 3.3, PostgreSQL
                - Frontend: React + Vite + Tailwind
                - Infra: Kubernetes on GKE

                ## Key Design Decisions
                - Use event-sourcing for audit trail
                - JWT auth, 1h expiry, refresh tokens in httpOnly cookies
                - All API calls go through the API Gateway for rate limiting

                ## Known Issues
                - Search is full-table-scan right now — needs indexing (tracked in #42)
                - Image uploads > 5MB fail in dev — S3 config issue

                ## Links
                - Repo: https://github.com/example/project-alpha
                - Staging: https://staging.example.com
                """);
    }

    private void writeIfAbsent(String name, String content) throws IOException {
        Path path = Path.of(docsDirectory).resolve(name);
        if (!Files.exists(path)) {
            Files.writeString(path, content, StandardOpenOption.CREATE_NEW);
        }
    }

    // ─── System prompt ────────────────────────────────────────────

    private static final String SYSTEM_PROMPT = """
            You are a helpful document assistant with access to a local document store.

            CAPABILITIES:
            - You can list available documents using the listDocuments tool.
            - You can read documents using the readDocument tool.
            - You can edit existing documents using the editDocument tool (full content replacement).
            - You can create new documents using the createDocument tool.

            GUIDELINES:
            - When a user references a document (by name or via @mention), use the tools to read it
              if its content hasn't already been provided inline.
            - When editing, preserve existing formatting and structure unless the user asks you to change it.
            - When you edit a document, confirm what changes you made.
            - Be concise but thorough. If a document is long, summarise the key parts first.
            - If a document doesn't exist, let the user know and offer to create it.

            CONTEXT:
            Some messages will contain inline document content prefixed with:
              "── Context: <filename> ──"
            This means the file was auto-loaded from an @mention. Use this content directly
            instead of calling readDocument for that file.
            """;
}