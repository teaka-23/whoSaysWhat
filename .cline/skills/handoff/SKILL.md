---
name: handoff
description: Compact the current conversation into a handoff document for another agent to pick up.
---

> **Project context (read first).** Use the project's actual version, commit,
> test and lint commands, and artifact paths; do not guess. Project rules
> override this skill. Reference files and commands instead of copying long
> logs. Redact secrets and personal data. Reply in the user's language.

Write a handoff document summarising the current conversation so a fresh agent can continue the work. Save to the temporary directory of the user's OS (`$TMPDIR`, else `/tmp`; `%TEMP%` on Windows) - not the current workspace.

Include a "suggested skills" section in the document, naming which skills the
next agent should read, with paths relative to the skills root (for example,
`grilling/SKILL.md`), not absolute paths.

Do not duplicate content already captured in other artifacts (specs, plans, ADRs, issues, commits, diffs). Reference them by path or URL instead.

Redact any sensitive information, such as API keys, passwords, or personally identifiable information.

If the user passed arguments, treat them as a description of what the next session will focus on and tailor the doc accordingly.
