# Design System — spring-ai-agent (chat UI)

Governed by `constitution.md` anti-slop rules. Pin tokens BEFORE screens. Reference taste: Linear's calm density + a terminal's honesty. Reuse this token block verbatim in the UI prompt.

## Tokens
```css
:root {
  /* Type — NOT Inter/Roboto/Arial */
  --font-sans: "IBM Plex Sans", ui-sans-serif, system-ui, sans-serif;
  --font-mono: "IBM Plex Mono", ui-monospace, "SF Mono", monospace; /* tool chips, code */
  --fs-300: 0.8125rem; --fs-400: 0.9375rem; --fs-500: 1.125rem; --fs-700: 1.5rem;
  --lh: 1.5; --tracking-tight: -0.01em;

  /* Color — warm-neutral ramp + ONE accent (teal). Real backdrop, not #fff. */
  --bg:        #14161a;   /* app backdrop (dark) */
  --surface:   #1c1f25;   /* bubbles, input */
  --surface-2: #23272e;
  --border:    #2c313a;
  --text:      #e7eaee;
  --text-dim:  #9aa3af;
  --accent:    #2dd4bf;   /* teal — user bubble, focus, send */
  --accent-ink:#06302b;   /* text on accent */
  --danger:    #f87171;

  /* Shape & elevation by ROLE (not uniform 16px-on-everything) */
  --r-bubble: 14px; --r-input: 10px; --r-chip: 6px;
  --shadow-pop: 0 6px 20px -8px rgba(0,0,0,.55);

  /* Spacing — 4/8pt grid */
  --s1:4px; --s2:8px; --s3:12px; --s4:16px; --s5:24px; --s6:32px;
}
```

## Rules
- Two faces max: IBM Plex Sans (UI), IBM Plex Mono (tool chips / numbers).
- One accent (teal). Neutral dark ramp otherwise. No gradients, no gradient text.
- Icons: inline single SVG set (Lucide), one stroke width, ~18px. No emoji-as-icons.
- User bubble = accent bg + accent-ink text, right-aligned. Assistant = `--surface` + `--text`, left.
- Tool chips: `--font-mono`, `--fs-300`, `--surface-2` bg, `--r-chip`.
- WCAG AA: body/accent contrast ≥ 4.5:1, visible teal focus ring, full keyboard nav (Enter sends, Shift+Enter newline), icon buttons get `aria-label`, touch targets ≥ 44px.
- Every state designed (empty/loading/error/success) — see `design.md`.
- Real example prompts in empty state, never lorem ipsum.
