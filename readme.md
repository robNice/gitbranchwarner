# Branch Warner

**Branch Warner** is a lightweight PhpStorm plugin that visually warns you when you are working on a “dangerous” Git branch (e.g., `live`, `prod`, `production`).

Instead of relying on the branch name in the status bar, the plugin displays a clearly visible red badge inside the editor — always anchored within the active editor window.

---

## Features

- **Warning Badge in the Editor**  
  A red badge containing the current branch name (“LIVE”, “PROD”, …) displayed as an overlay inside the editor.

- **Per Project**  
  The warning logic runs per project. Only the active editor of the relevant project is marked.

- **Configurable Warning Branches**  
  Comma-separated list of branch names that should be treated as “critical” (default: `live,prod,production`).

- **Customizable Position**  
  Selectable badge position:
    - Top left
    - Top right
    - Bottom left
    - Bottom right (default)

- **Scalable Size**  
  Adjustable badge size in levels (`1`–`5`).

- **Supports Git4Idea**  
  Uses the JetBrains Git API if available; otherwise falls back to reading `.git/HEAD` directly.

---

## Usage

### 1. Configure the warning branches

1. Open `Settings` → `Other Settings` → **Branch Warner** (or search for “Branch Warner”).
2. Fields:
    - **Warn branches (comma separated)**  
      Comma-separated list of branch names, e.g.:  
      `live,prod,production`
    - **Badge size (1–5)**  
      Scales the badge (1 = small, 5 = large).
    - **Badge position**  
      Drop-down with:
        - Bottom right (default)
        - Bottom left
        - Top right
        - Top left

3. Apply with `Apply` / `OK`.

### 2. Behavior inside the editor

- When the currently checked-out branch matches any configured warning branch:
    - A red badge with the branch name appears in the active editor.
    - When switching branches or switching projects, the badge updates or disappears accordingly.
- The badge “sticks” to the selected corner of the visible editor area even while scrolling.

---

## Compatibility

- Developed and tested with:
    - PhpStorm 2025.x
    - Java 17
    - Gradle 8/9
- Requires:
    - `com.intellij.modules.platform`
    - Optional: `Git4Idea` (if Git integration is enabled in the IDE)

The plugin operates per project — worktrees or multi-repo setups may depend on the current `.git/HEAD` or Git4Idea state.

---

