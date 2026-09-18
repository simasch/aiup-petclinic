#!/usr/bin/env bash
#
# PostToolUse(every tool) — notice a Status: line that changed, whoever changed it.
#
# guard-spec-status.sh sees an Edit or a Write before it happens; a sed, a
# heredoc or a script goes past it. So this one does not look at the tool call
# at all: after every call it reads the specifications themselves, compares
# each Status: with the value last seen, and reports one that now asserts
# Done / Tested / Automated without a coverage audit behind it. It reports
# once per change — the new value becomes the one last seen either way — and
# the Stop hook then holds the turn until the sensors have judged the claim.
set -euo pipefail
. "$(dirname "$0")/lib.sh"

cat >/dev/null # the input does not matter; the state of the repository does

gitdir=$(aiup_git_dir) || exit 0
snapshot="$gitdir/aiup-spec-status"

current=$(aiup_status_lines) || exit 0
if [ -f "$snapshot" ]; then
    previous=$(cat "$snapshot")
    seen=true
else
    previous=""
    seen=false # first sight — nothing to compare against
fi
printf '%s\n' "$current" > "$snapshot"
[ "$seen" = true ] || exit 0

flagged=""
while IFS=$'\t' read -r path value; do
    [ -n "$path" ] || continue
    aiup_asserting_status "$value" || continue
    old=$(awk -F'\t' -v p="$path" '$1 == p { print $2 }' <<<"$previous")
    [ "$old" = "$value" ] && continue
    id=$(aiup_spec_id "$path") || continue
    aiup_coverage_checked "$gitdir" "$id" && continue
    flagged="$flagged  $path: \"${old:-<new file>}\" -> \"$value\""$'\n'
done <<<"$current"
[ -n "$flagged" ] || exit 0

cat >&2 <<MSG
A Status: line changed without a coverage audit behind it:

$flagged
That line is an assertion the traceability sensors act on, not a label. Either
put the previous value back, or run

  aiup-vaadin-jooq:coverage-check <id>

and set the status again once it reports no gaps. Then run the sensors
(./mvnw -q test -Dgroups=sensor) so the one it switched on actually votes.
MSG
exit 2
