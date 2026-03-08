# MCP Document Chatbot — Spring AI
### Branch: `without-mcp/spring-ai-tool-annotation`

> ⚠️ **This branch does NOT use a real MCP Server.**
> It is intentionally built that way — the goal is to teach you **what MCP replaces**
> before you learn MCP itself. See the `with-mcp/spring-ai-mcp-client` branch for the
> real MCP implementation.

This is a CLI chatbot that simulates MCP-style tool calling using Spring AI's `@Tool` annotation.
The LLM can read and edit local documents, and users can use `@filename` syntax to
automatically inject document content as context — all running inside a single JVM process,
with no separate MCP Server involved.

**What you'll learn from this branch:**
- How tools are exposed to an LLM (the concept behind MCP Tools)
- How document context gets injected into a prompt (the concept behind MCP Resources)
- How Spring AI manages the agentic tool-call loop automatically
- Why a standard protocol like MCP becomes necessary at scale

---https://github.com/muhammedfahadt/mcp-fundamentals/blob/demo/mcp-doc-chatbot/OLLAMA_SETUP.md

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     CLI (Terminal)                       │
│   User types: "Summarise @meeting-notes.md"              │
└───────────────────────┬─────────────────────────────────┘
                        │
            ┌───────────▼──────────────┐
            │  DocumentContextInjector  │  ← scans for @mentions
            │  • finds @meeting-notes.md│    reads file content
            │  • injects content inline │    appends to message
            └───────────┬──────────────┘
                        │
            ┌───────────▼──────────────┐
            │       ChatClient          │  ← Spring AI
            │  • System prompt          │
            │  • Conversation memory    │
            │  • Tool schemas attached  │
            └───────────┬──────────────┘
                        │  HTTP (JSON)
            ┌───────────▼──────────────┐
            │      OpenAI / LLM         │  ← decides what to do
            │  • Reads injected context │
            │  • OR calls tool to read  │
            │  • OR calls tool to edit  │
            └───────────┬──────────────┘
                        │  tool_call response
            ┌───────────▼──────────────┐
            │      DocumentTools        │  ← MCP tools layer
            │  • listDocuments()        │    Spring AI executes
            │  • readDocument(name)     │    tool calls, feeds
            │  • editDocument(name,txt) │    results back to LLM
            │  • createDocument(name,…) │
            └──────────────────────────┘
                        │
                  ./docs/*.md / *.txt    ← local filesystem
```

### Two Ways Documents Reach the LLM

| Method | When | How |
|--------|------|-----|
| **@mention injection** | User tags a file: `@todo.txt` | File content added directly to user message *before* LLM call |
| **Tool call** | LLM decides it needs a file | LLM emits `readDocument("todo.txt")` → Spring AI runs it → result fed back |

The @mention approach is faster (one LLM round-trip).
Tool calls give the LLM agency to fetch what it needs dynamically.

---

## Quick Start

### 1. Prerequisites
- Java 21+
- Maven 3.9+
- OpenAI API key (`gpt-4o-mini` is used by default)

### 2. Clone & configure
```bash
# Set your OpenAI key
export OPENAI_API_KEY=sk-...

# (optional) point to your own docs folder
export DOCS_DIRECTORY=./my-docs
```

### 3. Run
```bash
cd mcp-doc-chatbot
mvn spring-boot:run
```

On first run, three sample documents are created automatically in `./docs/`:
- `meeting-notes.md`
- `todo.txt`
- `project-notes.md`

---

## Usage

```
you> /docs                                  # list available documents
you> Summarise @meeting-notes.md            # auto-inject context + summarise
you> What tasks are left in @todo.txt?      # check status
you> Edit @project-notes.md — add a "Deployment" section with k8s steps
you> Create a new document called retro.md with a sprint retrospective template
you> What do both @meeting-notes.md and @todo.txt say about the API?
```

### Slash Commands
| Command | Description |
|---------|-------------|
| `/docs` | List documents (local, no LLM call) |
| `/clear` | Clear terminal |
| `/help` | Show help |
| `/exit` | Quit |

---

## Key Concepts Demonstrated

### 1. MCP Tools (`DocumentTools.java`)
Each `@Tool`-annotated method is an MCP tool:
- Spring AI serializes the method signature into a **JSON schema**
- The schema is sent to the LLM with every request
- The LLM can invoke any tool at any reasoning step
- Spring AI intercepts the tool call, runs the Java method, feeds the result back

This is equivalent to what an MCP Server exposes — the difference is in a real
MCP setup the tools live in a separate process with a standardized protocol.
Spring AI `@Tool` is MCP-in-process.

### 2. Context Injection (`DocumentContextInjector.java`)
The `@mention` pattern mimics how tools like Cursor inject file content via MCP Resources.
Instead of waiting for the LLM to call `readDocument`, we push the content proactively.

### 3. Conversation Memory
`MessageChatMemoryAdvisor` keeps the full conversation history in memory and
automatically appends it to each request. The LLM can refer back to earlier messages.

---

## Swapping the LLM Provider

Because Spring AI abstracts the model layer, swapping providers is a one-line change in `pom.xml`:

```xml
<!-- OpenAI (default) -->
<artifactId>spring-ai-openai-spring-boot-starter</artifactId>

<!-- Anthropic Claude -->
<artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>

<!-- Local (Ollama) -->
<artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
```
## Documentation
- [Setup & Installation Ollama](mcp-doc-chatbot/OLLAMA_SETUP.md)

Update `application.yml` with the corresponding config key, and you're done.
No changes to `DocumentTools`, `CliChatRunner`, or any business logic.

---

## Project Structure

```
mcp-doc-chatbot/
├── pom.xml
├── docs/                            # document store (auto-created)
│   ├── meeting-notes.md
│   ├── todo.txt
│   └── project-notes.md
└── src/main/
    ├── java/com/example/mcpchat/
    │   ├── McpChatbotApplication.java    # Spring Boot entry point
    │   ├── DocumentTools.java            # MCP tools (@Tool methods)
    │   ├── DocumentContextInjector.java  # @mention → context injection
    │   └── CliChatRunner.java            # CLI loop + ChatClient setup
    └── resources/
        └── application.yml
```