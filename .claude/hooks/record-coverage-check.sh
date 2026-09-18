#!/usr/bin/env bash
#
# SubagentStop(uc-coverage) — remember that a coverage audit finished.
#
# guard-spec-status.sh and check-spec-status.sh accept an asserting Status:
# only after aiup-vaadin-jooq:coverage-check has audited that specification,
# and the audit runs in the plugin's read-only uc-coverage agent. Its stop is
# the one moment the session knows the audit happened, so this writes a marker
# per id the agent was asked about. The marker says an audit finished, not that
# it found no gaps: the sensors judge the claim itself on the next run.
set -euo pipefail
. "$(dirname "$0")/lib.sh"

input=$(cat)

case "$(jq -r '.agent_type // ""' <<<"$input" 2>/dev/null)" in
    *uc-coverage) ;;
    *) exit 0 ;;
esac
# An agent cut off by its token limit has audited nothing.
[ "$(jq -r '.stop_reason // "end_turn"' <<<"$input" 2>/dev/null)" = "end_turn" ] || exit 0

# UC-001, uc001, TC 1 … → UC-001 / TC-001, one per line.
ids() {
    grep -oiE '(UC|TC)[- ]?[0-9]{1,3}' | tr '[:lower:]' '[:upper:]' \
        | sed -E 's/^(UC|TC)[- ]?0*([0-9]+)$/\1 \2/' \
        | awk '{ printf "%s-%03d\n", $1, $2 }' | sort -u
}

# The assignment is the first user message of the agent's own transcript. The
# report's heading names the same id and stands in when the transcript lags.
transcript=$(jq -r '.agent_transcript_path // ""' <<<"$input" 2>/dev/null) || transcript=""
transcript="${transcript/#\~/$HOME}"
found=""
if [ -n "$transcript" ] && [ -f "$transcript" ]; then
    found=$(jq -c 'select(.type == "user") | .message.content
                   | if type == "string" then . else map(.text? // "") | join(" ") end' \
                "$transcript" 2>/dev/null | head -1 | ids || true)
fi
if [ -z "$found" ]; then
    found=$(jq -r '.last_assistant_message // ""' <<<"$input" 2>/dev/null \
                | grep -E '^## (UC|TC)-[0-9]{3}' | ids || true)
fi
[ -n "$found" ] || exit 0

gitdir=$(aiup_git_dir) || exit 0
mkdir -p "$gitdir/aiup-coverage-checked"
while IFS= read -r id; do
    [ -n "$id" ] && touch "$gitdir/aiup-coverage-checked/$id"
done <<<"$found"
exit 0
