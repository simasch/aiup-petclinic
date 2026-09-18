#!/usr/bin/env bash
#
# Stop — refuse to end the turn while changed code, specs or hooks are unverified.
#
# The sensors in src/test/java are post-hoc: they can only catch drift once
# someone runs them. Nothing in the repository can express "the agent ran them
# before it claimed to be done", because that is a property of the session, not
# of the source tree. This hook is that property — for the sensors over src/,
# docs/ and pom.xml, and for smoke.sh over the hooks themselves.
#
# It blocks once per turn. After a block Claude continues, runs what is asked,
# and re-enters Stop with stop_hook_active set; letting that second Stop
# through is what keeps a session that truly cannot build from looping forever.
set -euo pipefail
. "$(dirname "$0")/lib.sh"

input=$(cat)

active=$(jq -r '.stop_hook_active // false' <<<"$input" 2>/dev/null) || exit 0
[ "$active" = "true" ] && exit 0

cd "$(aiup_root)"
gitdir=$(aiup_git_dir) || exit 0
message=""

changed=$(aiup_changed_files "$gitdir") || exit 0
if [ -n "$changed" ]; then
    reason=$(aiup_unverified "$gitdir/aiup-sensors-ran" "$changed" "the sensors have")
    if [ -z "$reason" ]; then
        # The marker was written on the strength of the reports; a `clean`
        # since has taken the evidence away.
        since=$(printf '%s\n' "$changed" | aiup_newest_mtime)
        proof=$(aiup_sensor_reports_ok "$since") || reason="the evidence of that run is gone: $proof."
    fi
    if [ -n "$reason" ]; then
        message+="This session changed code, specifications or the build, and $reason

Changed in this session:
$(sed 's/^/  /' <<<"$changed")

Run the sensors before finishing:

  ./mvnw -q test -Dgroups=sensor

That is ArchitectureTest, TestLayerConventionsTest and the three traceability
sensors that hold docs/ and the code together — seconds, no Docker. A full
./mvnw test or verify counts too. What counts is the fresh, green Surefire
report of every sensor, so a narrowed run (-Dtest=…), a skipped one, a failed
one, or one still running in the background does not — and neither does output
that merely looks clean. Run it in the foreground and let it finish.

"
    fi
fi

hooks=$(aiup_changed_hooks "$gitdir") || exit 0
if [ -n "$hooks" ]; then
    reason=$(aiup_unverified "$gitdir/aiup-smoke-ran" "$hooks" "their smoke test has")
    if [ -n "$reason" ]; then
        message+="This session changed the hooks, and $reason

Changed in this session:
$(sed 's/^/  /' <<<"$hooks")

Run their smoke test before finishing:

  ./.claude/hooks/smoke.sh

"
    fi
fi

[ -n "$message" ] || exit 0
printf '%s' "$message" >&2
exit 2
