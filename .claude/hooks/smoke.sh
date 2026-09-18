#!/usr/bin/env bash
#
# Exercises the hooks against a throwaway repository. Run it after changing
# any of them:  ./.claude/hooks/smoke.sh
#
# It is a shell script and not a Maven test on purpose: the hooks are a
# property of a Claude Code session, and this is the closest thing to one.
# When every check passes it records that in this repository's git directory
# (aiup-smoke-ran), which is what the Stop hook asks for after a hook changed.
set -euo pipefail
hooks=$(cd "$(dirname "$0")" && pwd)
repo=$(cd "$hooks/../.." && pwd)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

cd "$tmp"
git init -q -b main
git config user.email hooks@example.com
git config user.name hooks
mkdir -p src/main/java docs/use_cases docs/test_cases .claude/hooks
echo a > src/main/java/A.java
echo d > docs/D.md
printf '# UC-001\n\n**Status:** Draft\n' > docs/use_cases/UC-001-x.md
printf '# UC-002\n\n**Status:** Done\n' > docs/use_cases/UC-002-y.md
echo '<project/>' > pom.xml
echo h > .claude/hooks/h.sh
git add -A && git commit -qm base
export CLAUDE_PROJECT_DIR="$tmp"
gitdir=$(git rev-parse --absolute-git-dir)
marker="$gitdir/aiup-sensors-ran"
checked="$gitdir/aiup-coverage-checked"

fails=0
check() { # name expected actual
    if [ "$2" = "$3" ]; then echo "ok   $1"; else echo "FAIL $1 (expected $2, got $3)"; fails=$((fails + 1)); fi
}
run() { # hook json → exit code
    set +e; printf '%s' "$2" | "$hooks/$1" >/dev/null 2>&1; local code=$?; set -e; echo "$code"
}
present() { if [ -e "$1" ]; then echo present; else echo absent; fi; }
reports() { # green | failed | none — the Surefire reports of the five sensors
    local dir="$tmp/target/surefire-reports" name
    rm -rf "$dir"; [ "$1" = none ] && return 0
    mkdir -p "$dir"
    for name in ArchitectureTest TestLayerConventionsTest UseCaseTraceabilityTest TestCaseTraceabilityTest BusinessRuleTraceabilityTest; do
        if [ "$1" = green ]; then
            printf '<?xml version="1.0"?>\n<testsuite name="x.%s" time="0.1" tests="3" errors="0" skipped="0" failures="0">\n</testsuite>\n' "$name"
        else
            printf '<?xml version="1.0"?>\n<testsuite name="x.%s" time="0.1" tests="3" errors="0" skipped="0" failures="1">\n</testsuite>\n' "$name"
        fi > "$dir/TEST-ai.unifiedprocess.petclinic.$name.xml"
    done
}
full='{"tool_input":{"command":"./mvnw -q test"},"tool_response":{"stdout":"","stderr":""}}'
sensors='{"tool_input":{"command":"./mvnw -q test -Dgroups=sensor"},"tool_response":{"stdout":""}}'
edit() { # file old new → PreToolUse Edit json
    printf '{"tool_name":"Edit","tool_input":{"file_path":"%s","old_string":"%s","new_string":"%s"}}' "$1" "$2" "$3"
}
write() { # file content → PreToolUse Write json
    printf '{"tool_name":"Write","tool_input":{"file_path":"%s","content":"%s"}}' "$1" "$2"
}

echo "-- session start"
touch "$marker" "$gitdir/aiup-smoke-ran"; mkdir -p "$checked"; touch "$checked/UC-001"
check "exits 0" 0 "$(run session-start.sh '{"source":"startup"}')"
check "records HEAD" "$(git rev-parse HEAD)" "$(cat "$gitdir/aiup-session-base")"
check "clears the sensor marker" absent "$(present "$marker")"
check "clears the smoke marker" absent "$(present "$gitdir/aiup-smoke-ran")"
check "clears the coverage markers" absent "$(present "$checked")"
check "snapshots the status lines" "docs/use_cases/UC-002-y.md	Done" "$(grep UC-002 "$gitdir/aiup-spec-status")"

echo "-- sensors: what counts as a run"
check "clean tree is free" 0 "$(run require-sensors.sh '{}')"
sleep 0.1
echo b > src/main/java/A.java
check "edit without a run blocks" 2 "$(run require-sensors.sh '{}')"
check "re-entry lets go" 0 "$(run require-sensors.sh '{"stop_hook_active":true}')"
reports none
run record-sensor-run.sh "$full" >/dev/null
check "clean output without reports is no evidence" absent "$(present "$marker")"
run record-sensor-run.sh '{"tool_input":{"command":"./mvnw test-compile"},"tool_response":{"stdout":""}}' >/dev/null
check "test-compile leaves no marker" absent "$(present "$marker")"
reports failed
run record-sensor-run.sh "$full" >/dev/null
check "a failed sensor leaves no marker" absent "$(present "$marker")"
reports green
sleep 0.1
echo b2 > src/main/java/A.java
run record-sensor-run.sh "$full" >/dev/null
check "reports older than the change leave no marker" absent "$(present "$marker")"
check "still blocked after those" 2 "$(run require-sensors.sh '{}')"
sleep 0.1
reports green
run record-sensor-run.sh "$sensors" >/dev/null
check "fresh green reports write the marker" present "$(present "$marker")"
check "free after a green run" 0 "$(run require-sensors.sh '{}')"
reports none
check "reports removed since block again" 2 "$(run require-sensors.sh '{}')"
reports green
run record-sensor-run.sh "$sensors" >/dev/null
check "free again with the reports back" 0 "$(run require-sensors.sh '{}')"

echo "-- sensors: what counts as a change"
sleep 0.1
echo c > src/main/java/A.java
check "edit after the run blocks" 2 "$(run require-sensors.sh '{}')"
sleep 0.1; reports green; run record-sensor-run.sh "$full" >/dev/null
check "free again after another run" 0 "$(run require-sensors.sh '{}')"
sleep 0.1
echo e > docs/D.md
git commit -qam change
check "a commit does not clear the guard" 2 "$(run require-sensors.sh '{}')"
sleep 0.1; reports green; run record-sensor-run.sh "$full" >/dev/null
check "free after a run over the commit" 0 "$(run require-sensors.sh '{}')"
git rm -q src/main/java/A.java
check "a deleted file counts" 2 "$(run require-sensors.sh '{}')"
sleep 0.1; reports green; run record-sensor-run.sh "$full" >/dev/null
check "free after a run over the deletion" 0 "$(run require-sensors.sh '{}')"
sleep 0.1
echo '<project></project>' > pom.xml
check "pom.xml counts" 2 "$(run require-sensors.sh '{}')"
sleep 0.1; reports green; run record-sensor-run.sh "$full" >/dev/null
check "free after a run over the pom" 0 "$(run require-sensors.sh '{}')"
sleep 0.1
run session-start.sh '{"source":"startup"}' >/dev/null
run record-sensor-run.sh "$full" >/dev/null
check "reports from before the session are no evidence" absent "$(present "$marker")"
sleep 0.1; reports green; run record-sensor-run.sh "$full" >/dev/null
check "free once this session ran them" 0 "$(run require-sensors.sh '{}')"

echo "-- hooks: smoke.sh is their sensor"
sleep 0.1
echo h2 > .claude/hooks/h.sh
check "a changed hook blocks" 2 "$(run require-sensors.sh '{}')"
printf '.claude/hooks/h.sh\n' > "$gitdir/aiup-smoke-ran"
check "free after a smoke run over it" 0 "$(run require-sensors.sh '{}')"

echo "-- status guard (before an Edit or Write)"
spec="$tmp/docs/use_cases/UC-001-x.md"
check "Draft to Done without an audit is refused" 2 "$(run guard-spec-status.sh "$(edit "$spec" '**Status:** Draft' '**Status:** Done')")"
check "mid-line edit is refused too" 2 "$(run guard-spec-status.sh "$(edit "$spec" 'Status:** Draft' 'Status:** Done')")"
check "Draft to Specified passes" 0 "$(run guard-spec-status.sh "$(edit "$spec" '**Status:** Draft' '**Status:** Specified')")"
check "Done to Draft passes" 0 "$(run guard-spec-status.sh "$(edit "$tmp/docs/use_cases/UC-002-y.md" '**Status:** Done' '**Status:** Draft')")"
check "unchanged status passes" 0 "$(run guard-spec-status.sh "$(edit "$spec" '**Status:** Done\nx' '**Status:** Done\ny')")"
check "new spec written as Done is refused" 2 "$(run guard-spec-status.sh "$(write "$tmp/docs/use_cases/UC-003-z.md" '# UC\n**Status:** Done\n')")"
check "new spec written as Draft passes" 0 "$(run guard-spec-status.sh "$(write "$tmp/docs/use_cases/UC-003-z.md" '# UC\n**Status:** Draft\n')")"
check "rewriting a Done spec as Done passes" 0 "$(run guard-spec-status.sh "$(write "$tmp/docs/use_cases/UC-002-y.md" '# UC-002\n\n**Status:** Done\nmore\n')")"
check "other documents pass" 0 "$(run guard-spec-status.sh "$(edit "$tmp/docs/architecture/x.md" '**Status:** Draft' '**Status:** Done')")"
mkdir -p "$checked"; touch "$checked/UC-001"
check "Draft to Done after an audit passes" 0 "$(run guard-spec-status.sh "$(edit "$spec" '**Status:** Draft' '**Status:** Done')")"
sleep 0.1
mkdir -p src/main/java && echo t > src/main/java/T.java
check "an audit older than a change to src/ is stale" 2 "$(run guard-spec-status.sh "$(edit "$spec" '**Status:** Draft' '**Status:** Done')")"
rm -rf "$checked" src/main/java/T.java

echo "-- status check (after any tool)"
run session-start.sh '{"source":"startup"}' >/dev/null
check "nothing changed: quiet" 0 "$(run check-spec-status.sh '{"tool_name":"Bash"}')"
sed -i.bak 's/Draft/Done/' "$spec" && rm -f "$spec.bak"
check "a sed to Done is reported" 2 "$(run check-spec-status.sh '{"tool_name":"Bash"}')"
check "reported once" 0 "$(run check-spec-status.sh '{"tool_name":"Bash"}')"
sed -i.bak 's/Done/Draft/' "$spec" && rm -f "$spec.bak"
check "back to Draft is quiet" 0 "$(run check-spec-status.sh '{"tool_name":"Bash"}')"
mkdir -p "$checked"; touch "$checked/UC-001"
sed -i.bak 's/Draft/Done/' "$spec" && rm -f "$spec.bak"
check "to Done after an audit is quiet" 0 "$(run check-spec-status.sh '{"tool_name":"Bash"}')"
printf '# TC-001\n\n**Status:** Automated\n' > docs/test_cases/TC-001-j.md
check "a new test case written as Automated is reported" 2 "$(run check-spec-status.sh '{"tool_name":"Write"}')"
rm -rf "$checked" docs/test_cases/TC-001-j.md

echo "-- coverage audit (SubagentStop of uc-coverage)"
transcript="$tmp/agent.jsonl"
printf '{"type":"user","message":{"role":"user","content":"UC-004 both"}}\n{"type":"assistant","message":{"role":"assistant","content":[{"type":"text","text":"## UC-004 Find Owners — implementation and tests"}]}}\n' > "$transcript"
audit() { printf '{"agent_type":"%s","stop_reason":"%s","agent_transcript_path":"%s","last_assistant_message":"%s"}' "$1" "$2" "$3" "$4"; }
run record-coverage-check.sh "$(audit aiup-vaadin-jooq:uc-coverage end_turn "$transcript" "")" >/dev/null
check "the audited id is recorded from the assignment" present "$(present "$checked/UC-004")"
rm -rf "$checked"
run record-coverage-check.sh "$(audit aiup-vaadin-jooq:uc-coverage end_turn /nowhere '## TC-001 Journey — implementation and tests')" >/dev/null
check "falls back to the report heading" present "$(present "$checked/TC-001")"
rm -rf "$checked"
run record-coverage-check.sh "$(audit aiup-vaadin-jooq:uc-coverage max_tokens "$transcript" "")" >/dev/null
check "an agent cut off records nothing" absent "$(present "$checked")"
run record-coverage-check.sh "$(audit Explore end_turn "$transcript" "")" >/dev/null
check "another agent records nothing" absent "$(present "$checked")"
printf '{"type":"user","message":{"role":"user","content":[{"type":"text","text":"Audit uc 7 and TC001, mode both"}]}}\n' > "$transcript"
run record-coverage-check.sh "$(audit plugin:aiup-vaadin-jooq:uc-coverage end_turn "$transcript" "")" >/dev/null
check "ids are normalized (uc 7)" present "$(present "$checked/UC-007")"
check "ids are normalized (TC001)" present "$(present "$checked/TC-001")"
rm -rf "$checked"

echo "-- worktree"
git worktree add -q "$tmp/wt" -b wt
(
    fails=0
    export CLAUDE_PROJECT_DIR="$tmp/wt"
    cd "$tmp/wt"
    echo z > docs/D.md
    sleep 0.1 # the report has to be strictly newer than the change, and mtimes are coarse on Linux
    reports() { :; }
    mkdir -p target/surefire-reports
    for name in ArchitectureTest TestLayerConventionsTest UseCaseTraceabilityTest TestCaseTraceabilityTest BusinessRuleTraceabilityTest; do
        printf '<testsuite name="x" tests="1" errors="0" skipped="0" failures="0">\n</testsuite>\n' > "target/surefire-reports/TEST-ai.unifiedprocess.petclinic.$name.xml"
    done
    run record-sensor-run.sh "$full" >/dev/null
    wtdir=$(git rev-parse --absolute-git-dir)
    check "marker lands in the worktree's git dir" present "$(present "$wtdir/aiup-sensors-ran")"
    check "stop is free after the run" 0 "$(run require-sensors.sh '{}')"
    exit "$fails"
) || fails=$((fails + $?))

echo
if [ "$fails" != 0 ]; then
    echo "$fails hook check(s) failed"
    exit 1
fi
echo "all hook checks passed"

# Record it for the repository these hooks belong to, so the Stop hook there
# knows the changed hooks were exercised.
export CLAUDE_PROJECT_DIR="$repo"
. "$hooks/lib.sh"
if repodir=$(aiup_git_dir); then
    aiup_changed_hooks "$repodir" > "$repodir/aiup-smoke-ran"
fi
