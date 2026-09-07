#!/bin/bash
# Builds a sandbox in which the release train can be run end to end without
# anything reaching the outside world: every publication goes to a maven
# repository on the loopback interface, every push goes to a bare clone next
# to it, and the release gates read that same local repository.
#
#   rehearsal/rehearse.sh <sandbox directory> [port]
#
# afterwards, from <sandbox>/bld:
#   ./bld release-train plan     bld+rife2+core
#   ./bld release-train local    bld+rife2+core
#   ./bld release-train publish  bld+rife2+core
#   ./bld release-train converge bld+rife2+core
set -euo pipefail

SB=${1:?usage: rehearse.sh <sandbox directory> [port]}
PORT=${2:-9111}
USER=rehearsal
PASSWORD=rehearsalsecret
REPO_URL="http://127.0.0.1:$PORT"
HERE=$(cd "$(dirname "$0")" && pwd)
# the members are siblings of the bld project this script lives in
W=$(cd "$HERE/../.." && pwd)

EXTENSIONS=(bld-antlr4 bld-archive bld-tests-badge bld-junit-reporter)
FOLLOWERS=(rife2-bld-hello)

rm -rf "$SB"
mkdir -p "$SB/remotes" "$SB/repo"

clone() { # name source
    git clone -q --bare "$2" "$SB/remotes/$1.git"
    git clone -q "$SB/remotes/$1.git" "$SB/$1"
}

echo "== cloning, with a bare clone as every remote"
git clone -q --bare "$W/rife2-core" "$SB/remotes/rife2-core.git"
clone bld "$W/bld"
clone rife2 "$W/rife2"
for e in "${EXTENSIONS[@]}" "${FOLLOWERS[@]}"; do
    if [ -d "$W/$e" ]; then
        clone "$e" "$W/$e"
    else
        echo "   $e isn't checked out locally, taking it from GitHub"
        git clone -q --bare "https://github.com/rife2/$e.git" "$SB/remotes/$e.git"
        git clone -q "$SB/remotes/$e.git" "$SB/$e"
    fi
done

echo "== redirecting the publications and the extension resolution"
# every name a project publishes to is redirected, derived from the project
# itself since the names aren't a convention every repository follows. One
# that is missed keeps whatever it was declared as, which the train's fencing
# then refuses for not being local
publication_properties() {
    local names
    names=$( { grep -rhoE 'repository\("[^"]+"\)' "$1/src/bld/java" 2>/dev/null |
                   sed 's/repository("//; s/")//'
               # everything declared for the user counts too: those are
               # destinations a project could publish to, and the train's
               # fencing refuses any of them that isn't local
               grep -hoE '^bld\.repo\.[^=.]+' "$HOME/.bld/bld.properties" 2>/dev/null |
                   sed 's/^bld\.repo\.//'
               printf 'rife2-releases\nrife2-snapshots\ncentral-releases\ncentral-snapshots\ngithub\n'
             } | sort -u )
    local name
    for name in $names; do
        printf 'bld.repo.%s=%s\n' "$name" "$REPO_URL"
        printf 'bld.repo.%s.username=%s\n' "$name" "$USER"
        printf 'bld.repo.%s.password=%s\n' "$name" "$PASSWORD"
    done
}

# the wrapper resolves its extensions through its own properties, so the
# loopback repository is added to the ones it already uses: the versions
# released during the rehearsal come from there, everything that was already
# public keeps coming from where it always did
redirect_wrapper() {
    local wrapper="$1/lib/bld/bld-wrapper.properties"
    [ -f "$wrapper" ] || return 0
    perl -0pi -e "s{^bld\.repositories=(.*)\$}{bld.repositories=\$1,rehearsal}m" "$wrapper"
    [ -n "$(tail -c1 "$wrapper")" ] && echo >> "$wrapper"
    cat >> "$wrapper" <<PROPS
bld.repo.rehearsal=$REPO_URL
bld.repo.rehearsal.username=$USER
bld.repo.rehearsal.password=$PASSWORD
PROPS
}

for r in bld rife2 "${EXTENSIONS[@]}" "${FOLLOWERS[@]}"; do
    publication_properties "$SB/$r" > "$SB/$r/local.properties"
    redirect_wrapper "$SB/$r"
done

# an extension whose tests assert on a report produced by building a sibling
# example expects that example to have been built at least once, which a
# checkout somebody has worked in already has and a fresh clone doesn't
echo "== priming example projects the extension tests read"
for e in "${EXTENSIONS[@]}"; do
    if [ -x "$SB/$e/example/bld" ]; then
        echo "   building the example of $e"
        (cd "$SB/$e/example" && ./bld download compile > /dev/null 2>&1 && ./bld test > /dev/null 2>&1) || true
    fi
done

echo "== putting both core checkouts on one commit that carries the overrides"
for r in bld rife2; do
    git -C "$SB/$r" config submodule.core.url "$SB/remotes/rife2-core.git"
    git -C "$SB/$r" -c protocol.file.allow=always submodule update --init -q
    git -C "$SB/$r/core" checkout -q main
done
publication_properties "$SB/bld/core" > "$SB/bld/core/local.properties"
redirect_wrapper "$SB/bld/core"
git -C "$SB/bld/core" add -A
git -C "$SB/bld/core" -c commit.gpgsign=false commit -qm "Rehearsal overrides"
git -C "$SB/bld/core" push -q origin HEAD:refs/heads/main
CORE_COMMIT=$(git -C "$SB/bld/core" rev-parse HEAD)
git -C "$SB/rife2/core" fetch -q "$SB/remotes/rife2-core.git" main
git -C "$SB/rife2/core" checkout -q --detach "$CORE_COMMIT"

# the one thing a local rehearsal can't reproduce is Maven Central holding a
# release. bld's own tests generate projects from the blueprint and resolve
# them from there, so in the sandbox the blueprint also offers the local
# repository, where the RIFE2 being released does exist
echo "== letting generated projects resolve the release locally"
BLUEPRINT=$SB/bld/src/main/java/rife/bld/blueprints/Rife2ProjectBlueprint.java
perl -0pi -e 's{import static rife\.bld\.dependencies\.Repository\.MAVEN_CENTRAL;}{import static rife.bld.dependencies.Repository.MAVEN_CENTRAL;\nimport static rife.bld.dependencies.Repository.MAVEN_LOCAL;}' "$BLUEPRINT"
perl -0pi -e 's{repositories = List\.of\(MAVEN_CENTRAL,}{repositories = List.of(MAVEN_LOCAL, MAVEN_CENTRAL,}' "$BLUEPRINT"

echo "== declaring the rehearsal in release-train.properties"
CONFIG="$SB/bld/release-train.properties"
cp "$W/bld/release-train.properties" "$CONFIG"
perl -0pi -e "s{^train\.releases\.repository=.*\$}{train.releases.repository=$REPO_URL}m" "$CONFIG"
perl -0pi -e "s{^train\.simulate=.*\$}{train.simulate=true}m" "$CONFIG"
cp "$W/bld/src/bld/java/rife/ReleaseTrainOperation.java" "$SB/bld/src/bld/java/rife/"
cp "$W/bld/src/bld/java/rife/BldBuild.java" "$SB/bld/src/bld/java/rife/"


for r in bld rife2 "${EXTENSIONS[@]}" "${FOLLOWERS[@]}"; do
    git -C "$SB/$r" add -A
    git -C "$SB/$r" -c commit.gpgsign=false commit -qm "Rehearsal overrides"
    git -C "$SB/$r" push -q origin HEAD:refs/heads/main
done

echo "== starting the repository on the loopback interface"
nohup java "$HERE/RepositoryServer.java" "$SB/repo" "$PORT" "$USER" "$PASSWORD" > "$SB/repository.log" 2>&1 &
echo $! > "$SB/repository.pid"
sleep 2
cat "$SB/repository.log"

cat <<DONE

sandbox ready at $SB
  repository  $REPO_URL  (log: $SB/repository.log, pid file: $SB/repository.pid)
  run from    $SB/bld

  ./bld release-train plan bld+rife2+core
  ./bld release-train local bld+rife2+core
  ./bld release-train publish bld+rife2+core
  ./bld release-train converge bld+rife2+core

stop the repository with: kill \$(cat $SB/repository.pid)
DONE
