# Contributing

## Commit messages

Follow Conventional Commits and write commit messages in English:

```text
type(optional scope): description
```

For breaking changes, explain in the commit body who is affected, how the behavior
changes, and what users need to do to migrate.

## Pull requests

Use PR stacks rooted on `main`. Each PR is normally squash-merged, so PR titles
must follow the same Conventional Commits format, English language requirement,
and scope rules as commit messages. Choose a title that describes the PR's overall
change, since it becomes the squash commit title and appears unchanged in the
changelog and release notes.

## Scopes

When a change affects only some targets, identify all affected targets in the
scope, separated by commas. Use loader names, Minecraft versions, or specific
version-loader pairs, depending on the scope of the change.

Omit the scope for changes that apply across the project.

Examples:

```text
fix(fabric): correct config registration
feat(26.3): add config screen support
refactor(1.20.1-forge,1.21.1-neo,1.21.1-forge): simplify config adapters
docs: clarify development setup
```
