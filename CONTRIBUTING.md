# 🤝 Contributing to Secure Media Vault

Thank you for your interest in contributing to **Secure Media Vault**! 🎉

This document provides guidelines and information about contributing to this project.

## 📋 Table of Contents

- [Code of Conduct](#-code-of-conduct)
- [Getting Started](#-getting-started)
- [Development Setup](#-development-setup)
- [Contributing Process](#-contributing-process)
- [Coding Standards](#-coding-standards)
- [Testing Guidelines](#-testing-guidelines)
- [Pull Request Process](#-pull-request-process)
- [Issue Reporting](#-issue-reporting)
- [Security](#-security)

## 📜 Code of Conduct

This project adheres to a Code of Conduct that we expect all contributors to follow:

- **Be respectful** and inclusive in all interactions
- **Be collaborative** and constructive in discussions
- **Be patient** with new contributors and questions
- **Be professional** in all communications

## 🚀 Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- **Java JDK 21+**
- **Docker & Docker Compose**
- **Git**
- **IDE** (IntelliJ IDEA recommended)

### Fork and Clone

1. **Fork** the repository on GitHub
2. **Clone** your fork locally:

```bash
git clone https://github.com/YOUR_USERNAME/secure-media-vault.git
cd secure-media-vault
```

3. **Add upstream** remote:

```bash
git remote add upstream https://github.com/jackby03/secure-media-vault.git
```

## 🛠️ Development Setup

### Environment Setup

1. **Copy environment template**:
```bash
cp .env.sample .env
```

2. **Start development environment**:
```bash
docker-compose -f docker-compose.dev.yml up -d
```

3. **Run the application**:
```bash
cd api
./gradlew bootRun
```

4. **Verify setup**:
```bash
curl http://localhost:8080/actuator/health
```

### IDE Configuration

#### IntelliJ IDEA Setup

1. **Import project** as Gradle project
2. **Set SDK** to Java 21
3. **Enable Kotlin** plugin
4. **Install recommended plugins**:
   - Spring Boot
   - Docker
   - Database Tools

#### Code Style

Import the code style configuration:
- File → Settings → Editor → Code Style → Kotlin
- Import scheme from `docs/kotlin-code-style.xml`

## 🔄 Contributing Process

### 1. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/issue-description
# or  
git checkout -b docs/documentation-update
```

### 2. Branch Naming Convention

- **Features**: `feature/add-file-encryption`
- **Bug fixes**: `fix/authentication-timeout`
- **Documentation**: `docs/api-documentation`
- **Tests**: `test/integration-tests`
- **Refactoring**: `refactor/service-layer`

### 3. Commit Guidelines

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```bash
# Feature
git commit -m "feat: add file encryption functionality"

# Bug fix
git commit -m "fix: resolve authentication timeout issue"

# Documentation
git commit -m "docs: update API documentation"

# Tests
git commit -m "test: add integration tests for file upload"

# Refactor
git commit -m "refactor: optimize database query performance"
```

#### Commit Message Format

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes
- `refactor`: Code refactoring
- `test`: Test additions or changes
- `chore`: Build process or auxiliary tool changes

## 💻 Coding Standards

### Kotlin Style Guide

```kotlin
// ✅ Good
class FileService(
    private val fileRepository: FileRepository,
    private val cacheService: CacheService
) {
    
    fun uploadFile(
        filename: String,
        content: ByteArray
    ): Mono<File> {
        return validateFile(filename, content)
            .flatMap { fileRepository.save(it) }
            .doOnSuccess { cacheService.invalidate("user-files") }
    }
    
    private fun validateFile(filename: String, content: ByteArray): Mono<File> {
        // Implementation
    }
}

// ❌ Bad
class fileservice(val repo:FileRepository,val cache:CacheService) {
    fun upload(f:String,c:ByteArray):Mono<File> {
        // Poor naming and formatting
    }
}
```

### Documentation Standards

```kotlin
/**
 * Uploads a file to the secure storage system
 *
 * @param filename The original filename
 * @param content The file content as byte array
 * @param ownerId The ID of the file owner
 * @return A Mono containing the uploaded file metadata
 * @throws SecurityException if file validation fails
 * @throws StorageException if storage operation fails
 */
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
fun uploadFile(
    filename: String,
    content: ByteArray,
    ownerId: UUID
): Mono<File>
```

### Error Handling

```kotlin
// ✅ Good - Specific error handling
return fileRepository.findById(id)
    .switchIfEmpty(Mono.error(FileNotFoundException("File not found: $id")))
    .onErrorMap(DataAccessException::class.java) { ex ->
        StorageException("Database access failed", ex)
    }

// ❌ Bad - Generic error handling
return fileRepository.findById(id)
    .onErrorReturn(null)
```

## 🧪 Testing Guidelines

### Test Structure

Follow the **AAA** pattern (Arrange, Act, Assert):

```kotlin
@Test
fun `should upload file successfully when valid input provided`() {
    // Arrange
    val filename = "test.pdf"
    val content = "test content".toByteArray()
    val ownerId = UUID.randomUUID()
    
    // Act
    val result = fileService.uploadFile(filename, content, ownerId)
    
    // Assert
    StepVerifier.create(result)
        .assertNext { file ->
            assertThat(file.filename).isEqualTo(filename)
            assertThat(file.ownerId).isEqualTo(ownerId)
            assertThat(file.status).isEqualTo(FileStatus.UPLOADED)
        }
        .verifyComplete()
}
```

### Test Categories

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test component interactions
3. **Contract Tests**: Test API contracts
4. **Performance Tests**: Test performance characteristics

### Test Coverage

- Maintain **minimum 80% code coverage**
- Focus on **business logic coverage**
- Include **edge cases and error scenarios**

### Running Tests

```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests "FileServiceTest"

# With coverage report
./gradlew test jacocoTestReport

# Integration tests only
./gradlew integrationTest
```

## 📥 Pull Request Process

### Before Creating a PR

1. **Sync with upstream**:
```bash
git fetch upstream
git rebase upstream/main
```

2. **Run tests locally**:
```bash
./gradlew clean test
```

3. **Check code style**:
```bash
./gradlew spotlessCheck
```

4. **Update documentation** if needed

### PR Template

When creating a PR, use our template:

```markdown
## 📋 Description
Brief description of changes

## 🔗 Related Issues
- Closes #123
- Related to #456

## 🚀 Type of Change
- [ ] 🐛 Bug fix
- [ ] ✨ New feature
- [ ] 💥 Breaking change
- [ ] 📚 Documentation update

## 🧪 Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

## 📸 Screenshots
If applicable, add screenshots

## 🔍 Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] Tests added/updated
```

### PR Review Process

1. **Automated checks** must pass
2. **Code review** by maintainers
3. **Testing verification**
4. **Documentation review**
5. **Final approval** and merge

## 🐛 Issue Reporting

### Bug Reports

Use our bug report template:

- **Clear title** describing the issue
- **Steps to reproduce** the problem
- **Expected vs actual behavior**
- **Environment details** (OS, Java version, etc.)
- **Logs and screenshots** if applicable

### Feature Requests

Use our feature request template:

- **Problem statement** being solved
- **Proposed solution** description
- **Use cases** and examples
- **Acceptance criteria**

### Security Issues

For security vulnerabilities:

- **DO NOT** create public issues
- **Email directly**: security@secure-media-vault.com
- **Include** detailed description and reproduction steps
- **Wait for** acknowledgment before disclosure

## 🔒 Security

### Security Best Practices

- **Never commit** sensitive data (passwords, keys, tokens)
- **Use environment variables** for configuration
- **Follow OWASP** security guidelines
- **Validate all inputs** thoroughly
- **Use parameterized queries** to prevent SQL injection

### Security Review Process

1. **Automated security scanning** with CodeQL
2. **Dependency vulnerability scanning** with Snyk
3. **Manual security review** for sensitive changes
4. **Penetration testing** for major features

## 📚 Additional Resources

### Documentation

- **Wiki**: [Technical documentation](https://github.com/jackby03/secure-media-vault/wiki)
- **API Docs**: [OpenAPI specification](docs/api.yaml)
- **Architecture**: [System design documents](docs/architecture/)

### Communication

- **Discussions**: [GitHub Discussions](https://github.com/jackby03/secure-media-vault/discussions)
- **Issues**: [GitHub Issues](https://github.com/jackby03/secure-media-vault/issues)
- **Email**: support@secure-media-vault.com

### Tools and Resources

- **Code Style**: [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Spring Boot**: [Official Documentation](https://spring.io/projects/spring-boot)
- **Testing**: [Reactor Test Documentation](https://projectreactor.io/docs/test/release/reference/)

---

## 🙏 Recognition

We appreciate all contributions, whether they are:

- **Code contributions**
- **Bug reports**
- **Feature suggestions**
- **Documentation improvements**
- **Community support**

Contributors will be recognized in our README and release notes.

---

**Thank you for contributing to Secure Media Vault!** 🎉

Your contributions help make this project better for everyone. If you have any questions, don't hesitate to reach out through our communication channels.

Happy coding! 💻✨
