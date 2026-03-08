# MCP FundamentalsIntro to Model Context Protocol (MCP). Learn to build MCP servers and clients with the spring-ai and Python SDK using tools, resources, and prompts. Covers architecture, transport-agnostic messaging, request-response flow, server testing with Inspector, document tools, resource handling, prompts, and practical integration patterns with Claude. 


The Model Context Protocol enables seamless communication between AI models and external tools, resources, and services, allowing for more sophisticated and context-aware AI applications.

## 🎯 Purpose

This repository demonstrates how to:

- **Build MCP Servers & Clients** using the Python SDK and Spring AI
- **Implement Tools & Resources** for dynamic AI interactions
- **Leverage Prompts** to guide AI model behavior
- **Design Enterprise-Grade Solutions** with best practices
- **Integrate with Claude** and other AI models
- **Ensure Scalability & Maintainability** in production environments

## 📚 What You'll Learn

### Core Concepts
- **MCP Architecture** – Understanding the Model Context Protocol design
- **Transport-Agnostic Messaging** – Building flexible communication layers
- **Request-Response Flow** – Mastering the MCP protocol lifecycle
- **Server Testing** – Using Inspector for robust validation

### Practical Components
- **Document Tools** – Creating tools that process and analyze documents
- **Resource Handling** – Managing dynamic and static resources efficiently
- **Prompts** – Designing effective system and user prompts
- **Integration Patterns** – Connecting MCP with Claude and other AI services

## 🛠️ Technology Stack

### Languages
- **Python** – MCP SDK implementation and tooling
- **Java/Spring Boot** – Enterprise MCP applications with Spring AI
- **Other JVM Languages** – Kotlin, Groovy, and more

### Frameworks & Libraries
- **Spring AI** – Simplified AI integration for Spring Boot applications
- **MCP Python SDK** – Official Python implementation
- **Claude API** – Integration with Anthropic's Claude models
- **Docker** – Containerization for deployment


## 🚀 Quick Start

### Prerequisites
- Python 3.8+ (for Python implementations)
- Java 17+ (for Spring AI implementations)
- Docker (optional, for containerized deployments)

### Python MCP Server

```bash
cd python/mcp-servers
pip install -r requirements.txt
python server.py
```

### Spring AI MCP Integration

```bash
cd spring-ai/mcp-spring-server
./gradlew bootRun
```

## 💡 Best Practices

### 1. **Server Design**
- Keep servers stateless for horizontal scaling
- Implement proper error handling and validation
- Use typed schemas for tools and resources
- Enable comprehensive logging

### 2. **Tool Development**
- Design tools with clear, single responsibilities
- Provide detailed schemas for AI model understanding
- Validate all inputs rigorously
- Return meaningful error messages

### 3. **Resource Management**
- Cache frequently accessed resources
- Implement efficient resource retrieval
- Use pagination for large datasets
- Monitor resource consumption

### 4. **Prompt Engineering**
- Define clear system prompts
- Include context in user prompts
- Use examples for complex behaviors
- Iterate and test extensively

### 5. **Integration & Deployment**
- Use environment variables for configuration
- Implement health checks and monitoring
- Enable graceful shutdown
- Use containerization for consistency
- Implement rate limiting and throttling

### 6. **Testing**
- Write unit tests for tools and utilities
- Create integration tests for server behavior
- Use MCP Inspector for interactive testing
- Test edge cases and error scenarios
- Maintain >80% code coverage

### 7. **Security**
- Validate all inputs and outputs
- Use authentication and authorization
- Protect sensitive credentials
- Implement request rate limiting
- Regular security audits

## 📖 Documentation

Detailed documentation is available in the `docs/` directory:

- **[Architecture Guide](docs/architecture.md)** – Deep dive into MCP design
- **[Getting Started](docs/getting-started.md)** – Step-by-step setup guide
- **[Best Practices](docs/best-practices.md)** – Patterns and recommendations
- **[API Reference](docs/api-reference.md)** – Comprehensive API documentation

## 🧪 Testing & Validation

### Using MCP Inspector

```bash
# Test your MCP server with interactive Inspector
mcp-inspect python/mcp-servers/server.py
```

### Running Tests

```bash
# Python tests
cd python && pytest tests/

# Spring AI tests
cd spring-ai && ./gradlew test
```

## 🔗 Integration Examples

### Claude Integration
```python
# Example: Connecting MCP server with Claude
from mcp import ClientSession
from claude_sdk import Anthropic

client = Anthropic()
session = ClientSession(server_params)
# Use tools via Claude with MCP context
```

### Spring Boot Integration
```java
// Example: Spring AI with MCP tools
@Configuration
public class MCPToolConfig {
    @Bean
    public MCPToolProvider mcpToolProvider() {
        return new MCPToolProvider(mcpServerConnection);
    }
}
```

## 📊 Examples Included

- ✅ File system document processor
- ✅ Database query tool
- ✅ Web search integration
- ✅ Real-time data resource handler
- ✅ Multi-step workflow automation
- ✅ Claude conversation with MCP tools

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Standards
- Follow PEP 8 for Python code
- Follow Google Java Style Guide for Java code
- Write meaningful commit messages
- Include tests with all changes
- Update documentation accordingly

## 📝 License

This project is licensed under the MIT License – see the `LICENSE` file for details.

## 🔗 Resources

- [Model Context Protocol Documentation](https://modelcontextprotocol.io/)
- [Claude API Documentation](https://docs.anthropic.com/)
- [Spring AI Documentation](https://spring.io/projects/spring-ai)
- [Python MCP SDK](https://github.com/modelcontextprotocol/python-sdk)
- [MCP GitHub Repository](https://github.com/modelcontextprotocol)

## 📞 Support & Contact

- **Issues** – [GitHub Issues](https://github.com/muhammedfahadt/mcp-fundamentals/issues)
- **Discussions** – [GitHub Discussions](https://github.com/muhammedfahadt/mcp-fundamentals/discussions)
- **Author** – [@muhammedfahadt](https://github.com/muhammedfahadt)

## 🎓 Learning Path

**Beginner** → Start with `docs/getting-started.md` and `python/examples/`

**Intermediate** → Explore `docs/architecture.md` and build your first server

**Advanced** → Implement Spring AI integration and production patterns

---

**Happy coding! Build amazing things with MCP.** 🚀
