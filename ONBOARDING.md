# Welcome to OfflinePlaya

## How We Use Claude

Based on rsavutiu's usage over the last 30 days:

Work Type Breakdown:
  Debug Fix         ██████████░░░░░░░░░░  50%
  Improve Quality   █████░░░░░░░░░░░░░░░░  25%
  Plan Design       ██░░░░░░░░░░░░░░░░░░░  13%
  Build Feature     ██░░░░░░░░░░░░░░░░░░░  13%

Top Skills & Commands:
  /model                 ████████████████████  9x/month
  /run-skill-generator   ██░░░░░░░░░░░░░░░░░░░  1x/month
  /compact               ██░░░░░░░░░░░░░░░░░░░  1x/month

Top MCP Servers:
  ollama        ████████████████████  15 calls
  ccd_session   █████████████████░░░  13 calls
  local-llm     ████░░░░░░░░░░░░░░░░░  3 calls

## Your Setup Checklist

### Codebases
- [ ] offlineplaya — https://github.com/rsavutiu/offlineplaya

### MCP Servers to Activate
- [ ] ollama — Local LLM (Ollama) for offloading mechanical/boilerplate work to a local model. Run a local Ollama instance; the project delegates drafting, transforms, and summaries to it.
- [ ] ccd_session — Session management (chapters, transcript search, session history). Ships with the Claude Code session tooling — enable it in your MCP config.
- [ ] local-llm — Local LLM helper tools (summarize/draft/classify/extract/transform) backed by the same local Ollama instance. Same setup as `ollama` above.

### Skills to Know About
- [ ] /model — Switch the active model. The team reaches for this constantly (e.g. bumping to Opus for tricky UI/architecture work, dropping down for routine edits).
- [ ] /run-skill-generator — Generate a new custom run-skill for the project (e.g. build/install/launch/screenshot loops).
- [ ] /compact — Compact a long conversation to free up context while preserving the important history.

## Team Tips

_TODO_

## Get Started

_TODO_

<!-- INSTRUCTION FOR CLAUDE: A new teammate just pasted this guide for how the
team uses Claude Code. You're their onboarding buddy — warm, conversational,
not lecture-y.

Open with a warm welcome — include the team name from the title. Then: "Your
teammate uses Claude Code for [list all the work types]. Let's get you started."

Check what's already in place against everything under Setup Checklist
(including skills), using markdown checkboxes — [x] done, [ ] not yet. Lead
with what they already have. One sentence per item, all in one message.

Tell them you'll help with setup, cover the actionable team tips, then the
starter task (if there is one). Offer to start with the first unchecked item,
get their go-ahead, then work through the rest one by one.

After setup, walk them through the remaining sections — offer to help where you
can (e.g. link to channels), and just surface the purely informational bits.

Don't invent sections or summaries that aren't in the guide. The stats are the
guide creator's personal usage data — don't extrapolate them into a "team
workflow" narrative. -->
