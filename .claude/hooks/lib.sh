#!/usr/bin/env bash
#
# Shared by the hooks in this directory. Nothing here may take a session down:
# a function that cannot answer returns non-zero, and the caller exits 0.
#
# The hooks keep their state in the git directory, so they follow a worktree
# rather than the shared .git of its parent:
#
#   aiup-session-base       the commit the session started on; its mtime is
#                           when the session started (session-start.sh)
#   aiup-sensors-ran        the change set the last green sensor run saw
#                           (record-sensor-run.sh); its mtime is when
#   aiup-smoke-ran          the hook change set the last green smoke.sh saw
#   aiup-spec-status        every UC/TC Status: line as last seen
#                           (check-spec-status.sh)
#   aiup-coverage-checked/  one file per UC/TC id whose coverage was audited
#                           in this session (record-coverage-check.sh)
#
# Two principles, both from ADR-011:
#   - a guard reads the state of the repository, never the shape of a tool
#     call, so it holds whichever tool made the change;
#   - evidence is positive — a Surefire report, an audit that finished — and
#     never the absence of failure text on a console.

export LC_ALL=C

aiup_root() {
    printf '%s' "${CLAUDE_PROJECT_DIR:-$(pwd)}"
}

aiup_git_dir() {
    git -C "$(aiup_root)" rev-parse --absolute-git-dir 2>/dev/null
}

# _aiup_changed <gitdir> <paths...>: every path under <paths> that differs
# from what the session started on: the working tree and the index against
# HEAD, plus whatever was committed since the session's base commit — a commit
# must not clear a guard. One path per line, sorted, unique, relative to the
# repository root. A path that no longer exists is suffixed "(deleted)", so a
# file removed after the last run differs from the one that run saw; whether
# it is staged is not a change.
_aiup_changed() {
    local gitdir="$1" root base
    shift
    root=$(aiup_root)
    {
        git -C "$root" -c core.quotePath=false status --porcelain --untracked-files=all -- "$@" 2>/dev/null \
            | sed 's/^...//; s/^.* -> //'
        if [ -f "$gitdir/aiup-session-base" ]; then
            base=$(cat "$gitdir/aiup-session-base")
            if git -C "$root" cat-file -e "$base^{commit}" 2>/dev/null; then
                git -C "$root" -c core.quotePath=false diff --name-only "$base" HEAD -- "$@" 2>/dev/null
            fi
        fi
    } | sed '/^$/d' | sort -u | while IFS= read -r path; do
        if [ -e "$root/$path" ]; then printf '%s\n' "$path"; else printf '%s (deleted)\n' "$path"; fi
    done
}

# What the sensors judge: code, specifications, and the build that runs them.
aiup_changed_files() {
    _aiup_changed "$1" src docs pom.xml
}

# What smoke.sh judges.
aiup_changed_hooks() {
    _aiup_changed "$1" .claude/hooks .claude/settings.json
}

# Fractional seconds where the platform offers them: an edit and the build that
# follows it can easily land in the same whole second. 0 for a missing file.
# GNU stat first: BSD stat rejects -c without printing anything, whereas GNU
# stat reads -f as "file system status" and prints that before failing.
aiup_mtime() {
    stat -c %.9Y "$1" 2>/dev/null || stat -f %Fm "$1" 2>/dev/null || echo 0
}

aiup_newer() {
    awk -v a="$1" -v b="$2" 'BEGIN { exit !(a > b) }'
}

# Newest mtime among the paths on stdin (relative to the root) that still
# exist; 0 when none does.
aiup_newest_mtime() {
    local root newest=0 path m
    root=$(aiup_root)
    while IFS= read -r path; do
        [ -f "$root/$path" ] || continue
        m=$(aiup_mtime "$root/$path")
        if aiup_newer "$m" "$newest"; then newest=$m; fi
    done
    printf '%s' "$newest"
}

# aiup_unverified <marker> <changed> <subject>: why the run recorded in
# <marker> does not cover <changed>. Prints nothing when it does.
aiup_unverified() {
    local marker="$1" changed="$2" subject="$3" unseen stamp file
    if [ ! -f "$marker" ]; then
        echo "$subject has not run in this session."
        return 0
    fi
    # Changes the last run did not see: added, deleted, or committed since.
    unseen=$(comm -23 <(printf '%s\n' "$changed") <(sort -u "$marker") || true)
    if [ -n "$unseen" ]; then
        printf 'these changes happened after the last run:\n%s\n' "$(sed 's/^/    /' <<<"$unseen")"
        return 0
    fi
    # Files the run did see, edited again since.
    stamp=$(aiup_mtime "$marker")
    while IFS= read -r file; do
        [ -f "$(aiup_root)/$file" ] || continue
        if aiup_newer "$(aiup_mtime "$(aiup_root)/$file")" "$stamp"; then
            echo "$file was changed after the last run."
            return 0
        fi
    done <<<"$changed"
}

# The classes whose Surefire reports prove that the sensors ran. All carry the
# JUnit tag "sensor", so `./mvnw test -Dgroups=sensor` runs exactly these.
AIUP_SENSORS="ArchitectureTest TestLayerConventionsTest UseCaseTraceabilityTest TestCaseTraceabilityTest BusinessRuleTraceabilityTest"

# aiup_sensor_reports_ok <since>: every sensor's Surefire report exists, is
# newer than <since>, ran at least one test, and had no failure or error.
# Prints why not, and returns 1, otherwise. The report is the evidence: it is
# written by the test JVM itself, per class, whatever the console showed.
aiup_sensor_reports_ok() {
    local since="$1" dir name report head tests failures errors
    dir="$(aiup_root)/target/surefire-reports"
    for name in $AIUP_SENSORS; do
        report="$dir/TEST-ai.unifiedprocess.petclinic.$name.xml"
        if [ ! -f "$report" ]; then
            echo "there is no Surefire report for $name"
            return 1
        fi
        if ! aiup_newer "$(aiup_mtime "$report")" "$since"; then
            echo "the Surefire report for $name predates the change"
            return 1
        fi
        head=$(grep -m1 -o '<testsuite[^>]*' "$report" || true)
        tests=$(sed -nE 's/.*[[:space:]]tests="([0-9]+)".*/\1/p' <<<"$head")
        failures=$(sed -nE 's/.*[[:space:]]failures="([0-9]+)".*/\1/p' <<<"$head")
        errors=$(sed -nE 's/.*[[:space:]]errors="([0-9]+)".*/\1/p' <<<"$head")
        if [ -z "$tests" ] || [ "$tests" = 0 ]; then
            echo "the Surefire report for $name shows no test"
            return 1
        fi
        if [ "${failures:-1}" != 0 ] || [ "${errors:-1}" != 0 ]; then
            echo "$name failed"
            return 1
        fi
    done
    return 0
}

# The value after "Status:**" on a line, trailing blanks removed. Tolerates a
# line fragment that starts mid-way, as an Edit's old_string may.
aiup_status_value() {
    sed -E 's/.*Status:\*\*[[:space:]]*//; s/[[:space:]]+$//' <<<"$1"
}

# A status that switches a traceability sensor on: Done / Tested on a use
# case, Automated on a test case.
aiup_asserting_status() {
    case "$1" in
        Done | Tested | Automated) return 0 ;;
        *) return 1 ;;
    esac
}

# UC-NNN / TC-NNN from a specification's path.
aiup_spec_id() {
    basename "$1" | grep -oE '^(UC|TC)-[0-9]{3}'
}

# Every UC/TC specification in the working tree and the value of its Status:
# line, "path<TAB>value" per line, sorted.
aiup_status_lines() {
    local path value
    (
        cd "$(aiup_root)" || exit 0
        for path in docs/use_cases/UC-*.md docs/test_cases/TC-*.md; do
            [ -f "$path" ] || continue
            value=$(aiup_status_value "$(grep -m1 'Status:\*\*' "$path" || true)")
            printf '%s\t%s\n' "$path" "$value"
        done
    ) | sort
}

# aiup_coverage_checked <gitdir> <id>: a coverage audit of <id> finished in
# this session, and nothing under src/ — the code and tests it judged — has
# changed since.
aiup_coverage_checked() {
    local gitdir="$1" id="$2" marker newest
    marker="$gitdir/aiup-coverage-checked/$id"
    [ -f "$marker" ] || return 1
    newest=$(aiup_changed_files "$gitdir" | grep '^src/' | aiup_newest_mtime)
    aiup_newer "$(aiup_mtime "$marker")" "$newest"
}
