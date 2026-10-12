Write a `CONTEXT.md` file for the current feature module.

This file is a **snapshot** — it captures WHY this module was built this way,
at the time it was built. It helps future developers (and AI) understand decisions
without guessing from code.

## Where to Create
Place the file inside the feature package:
```
feature/{feature_name}/CONTEXT.md
```

## Template — Fill Every Section

```markdown
# {Feature Name} — Implementation Context
> Written: {YYYY-MM-DD} | Author: {who built it}

## Business Context
Why does this module exist? What business problem does it solve?
(2-3 sentences max)

## Technical Decisions
List each important decision and WHY it was made:
- **{Decision}**: {reason}
- **{Decision}**: {reason}

## Considered and Rejected
Options that were evaluated but NOT chosen:
- **{Option}**: rejected because {reason}

## Dependencies
- Depends on: {list other modules this depends on}
- Depended by: {list modules that depend on this}

## Known Limitations
Things that are intentional trade-offs or not yet implemented:
- ⚠️ {limitation — explain why it's acceptable for now}

## Refactor Log
(Add entries here when significant changes are made. Do NOT edit sections above.)

### {YYYY-MM-DD} | {author}
- {what changed and why}
```

## Rules
1. Write in English
2. Be specific — "chose X because Y", not just "chose X"
3. Keep each section concise (2-5 bullet points max)
4. "Known Limitations" is critical — prevents future devs from "fixing" intentional trade-offs
5. NEVER edit old content — only ADD new entries in Refactor Log
6. Write it NOW while context is fresh — tomorrow you'll forget half the reasons