# Ollama Setup Guide

## Installation

### Windows
1. Download Ollama for Windows from: https://ollama.com/download/OllamaSetup.exe
2. Run the installer and follow the prompts
3. Restart your terminal/command prompt after installation

### Alternative: Using PowerShell
```powershell
# Download and install Ollama
Invoke-WebRequest -Uri "https://ollama.com/download/OllamaSetup.exe" -OutFile "$env:TEMP\OllamaSetup.exe"
Start-Process -FilePath "$env:TEMP\OllamaSetup.exe" -Wait
```

## Starting Ollama

After installation, start the Ollama service:

```bash
# Start Ollama service
ollama serve
```

## Pulling Models

Once Ollama is running, pull the model you want to use:

```bash
# Pull the llama2 model (used in our application)
ollama pull llama2

# Alternative models you can use:
ollama pull codellama    # For code-related tasks
ollama pull mistral      # For general purpose
ollama pull gemma        # For lightweight tasks
```

## Verifying Installation

Test that Ollama is working:

```bash
# Check if Ollama is running
ollama list

# Test with a simple chat
ollama run llama2 "Hello, how are you?"
```

## Troubleshooting

### Port Issues
If port 11434 is in use, you can specify a different port:
```bash
# Set custom port (add to application.yaml if needed)
export OLLAMA_HOST="0.0.0.0:11435"
ollama serve
```

### Windows Service
On Windows, Ollama should start automatically as a service. If not:
```bash
# Start as service
net start Ollama

# Or run manually in a terminal
ollama serve
```

## Using with the Application

Once Ollama is running and you have the model downloaded, our Spring AI application should connect automatically using the configuration in `application.yaml`.