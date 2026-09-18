#!/usr/bin/env bash
#
# SessionStart — remember where the session began.
#
# require-sensors.sh diffs the working tree against HEAD, which a commit would
# silently clear. Recording HEAD here lets it also diff HEAD against this
# commit, so "changed in this session" survives a `git commit`. The file's
# mtime is when the session started: a Surefire report older than that was
# written by another session and proves nothing about this one — so the
# markers of the previous session go, and the Status: lines are read once so
# that check-spec-status.sh has something to compare against.
set -euo pipefail
. "$(dirname "$0")/lib.sh"

gitdir=$(aiup_git_dir) || exit 0
head=$(git -C "$(aiup_root)" rev-parse HEAD 2>/dev/null) || exit 0
printf '%s\n' "$head" > "$gitdir/aiup-session-base"
rm -f "$gitdir/aiup-sensors-ran" "$gitdir/aiup-smoke-ran"
rm -rf "$gitdir/aiup-coverage-checked"
aiup_status_lines > "$gitdir/aiup-spec-status" || true
