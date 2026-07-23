# Contributing to SmithAI

Thank you for your interest in SmithAI! This project is currently driven by the repo owner, but suggestions and bug reports are welcome.

## How to contribute

1. **Bug reports** — Use `/smithai report` in-game or open an issue at https://github.com/Syntaxful/SmithAI/issues
2. **Feature ideas** — Open an issue with the `feature request` label
3. **Code** — If you have a small, focused fix, open a PR. Large changes are best discussed in an issue first.

## Project rules

- The user/owner is the only committer and the only listed contributor.
- `GITHUB_PERSONAL_ACCESS_TOKEN` is not used for automated commits without explicit authorization.
- Keep the plugin JAR small. Skills are generated at runtime, not hardcoded by the thousands.
- Prefer broad skill descriptions over item-specific recipes. The AI should reason about goals, not look up every block ID.
- All new code should build with `mvn -f SmithAI/pom.xml clean test package` and the Python server should pass `python -m py_compile SmithAI-Server/app.py`.
- Update `README.md`, `SKILLS.md`, or `HOSTING.md` if your change affects usage or setup.

## Development quick start

```bash
# Build the plugin
./build.sh

# Or with Maven
cd SmithAI && mvn clean test package

# Run the Python server tests
python -m py_compile SmithAI-Server/app.py
python SmithAI-Server/test_app.py

# Package a release
./package-release.sh 2.0.0
```

## Code style

- Java 17 target
- Bukkit/Paper API 1.21.x
- Python 3.10+ for SmithAI-Server
- Keep dependencies minimal and clearly documented in `requirements.txt` or `pom.xml`
