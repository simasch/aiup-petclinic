#!/usr/bin/env bash
#
# PreToolUse(Edit|Write) — a Status: line is an assertion, not a label.
#
# `Status: Done` / `Tested` / `Automated` switches a traceability sensor on, so
# setting one by hand either breaks the build or, worse, certifies coverage that
# was never checked. This refuses the edit unless a coverage audit of that
# specification finished in this session after the code and tests last changed
# (record-coverage-check.sh writes that evidence). An edit made through Bash
# never passes here; check-spec-status.sh reads the file itself afterwards.
set -euo pipefail
. "$(dirname "$0")/lib.sh"

input=$(cat)

# A hook must never take the session down with it: anything unexpected on
# stdin means this guard has nothing to say, not that the call should fail.
file=$(jq -r '.tool_input.file_path // ""' <<<"$input" 2>/dev/null) || exit 0

case "$file" in
    */docs/use_cases/UC-*.md | */docs/test_cases/TC-*.md) ;;
    *) exit 0 ;;
esac

# No line anchor: an Edit's old_string may start mid-line.
status_line() { grep -m1 'Status:\*\*' <<<"$1" || true; }

# Edit carries new_string, Write carries the whole file; either may hold it.
after=$(status_line "$(jq -r '.tool_input.new_string // .tool_input.content // ""' <<<"$input" 2>/dev/null)")
[ -n "$after" ] || exit 0

# An Edit says what it replaces; a Write replaces the file, so the file on
# disk is the "before" — rewriting a Done specification is not a new claim.
if jq -e '.tool_input | has("old_string")' <<<"$input" >/dev/null 2>&1; then
    before=$(status_line "$(jq -r '.tool_input.old_string' <<<"$input" 2>/dev/null)")
elif [ -f "$file" ]; then
    before=$(status_line "$(cat "$file")")
else
    before=""
fi
value=$(aiup_status_value "$after")
[ "$(aiup_status_value "$before")" = "$value" ] && exit 0

# Draft, Specified, a downgrade: no sensor switches on, nothing to prove.
aiup_asserting_status "$value" || exit 0

id=$(aiup_spec_id "$file") || exit 0
gitdir=$(aiup_git_dir) || exit 0
aiup_coverage_checked "$gitdir" "$id" && exit 0

cat >&2 <<MSG
Refused: $(basename "$file") would read "**Status:** $value".

That line is an assertion the traceability sensors act on, not a label, and no
coverage audit of $id has finished in this session since the code and tests
last changed. Run

  aiup-vaadin-jooq:coverage-check $id

first. When it reports no gaps, make this edit again and then run the sensors
(./mvnw -q test -Dgroups=sensor) so the one it switches on actually votes. When
it reports gaps, close them or leave the status as it is.
MSG
exit 2
