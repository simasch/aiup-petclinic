#!/usr/bin/env bash
#
# PostToolUse(Bash) — remember that the traceability sensors ran, and were green.
#
# Writes the marker require-sensors.sh reads: the change set this run saw, in a
# file whose mtime is when it saw it. The evidence is the Surefire report each
# sensor class writes (target/surefire-reports/TEST-*.xml), never the console:
# a report has to exist for every sensor, be newer than the session start and
# than every changed file, count tests, and count no failure or error. So a
# narrowed run (-Dtest=…) leaves the other reports stale, a skipped run writes
# none, a failed one says so in the file, and a run in the background or piped
# through tail has not written them when this hook looks. Output text that
# merely looks clean is not consulted.
set -euo pipefail
. "$(dirname "$0")/lib.sh"

input=$(cat)
command=$(jq -r '.tool_input.command // ""' <<<"$input" 2>/dev/null) || exit 0

# Only a Maven test or verify can have written reports; nothing else is worth
# a stat. `./mvnw -q clean test`, `mvn -B verify …` — but not `./mvnw test-compile`.
grep -Eq '(\./)?mvnw?([[:space:]]|$)' <<<"$command" || exit 0
grep -Eq '[[:space:]](test|verify)([[:space:]]|$)' <<<"$command" || exit 0
[ "$(jq -r '.tool_response.interrupted // false' <<<"$input" 2>/dev/null)" = "true" ] && exit 0

gitdir=$(aiup_git_dir) || exit 0
changed=$(aiup_changed_files "$gitdir")
since=$(printf '%s\n' "$changed" | aiup_newest_mtime)
started=$(aiup_mtime "$gitdir/aiup-session-base")
if aiup_newer "$started" "$since"; then since=$started; fi
aiup_sensor_reports_ok "$since" >/dev/null || exit 0
printf '%s\n' "$changed" > "$gitdir/aiup-sensors-ran"
