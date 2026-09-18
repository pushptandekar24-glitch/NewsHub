# Original project (reference only)

The three files that made up the uploaded `Basic-API-main` project, kept so you
can compare the old implementation against the migrated one.

| File | Was |
|---|---|
| `original-server.js` | the entire Express backend (148 lines, 4 NewsAPI proxy routes) |
| `original-index.html` | the entire frontend (344 lines, Tailwind CDN + vanilla JS) |
| `original-package.json` | express, cors, axios, nodemon |

**Do not run `original-server.js`.** Line 13 contains a hardcoded NewsAPI key
that should be treated as compromised — revoke and regenerate it at
newsapi.org. The key has been left in place here only so you can see exactly
what was committed; delete this folder once you have rotated it.

Where each route went is mapped in `../MIGRATION_PLAN.md` section 4.
