#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
stage="${1:-all}"
shift || true
maven="${TACHE2_MAVEN:-mvn}"
common=(-B -ntp -Pfast -DskipTests=false)
original='org.apache.tika.io.LookaheadInputStreamTest'
generated='org.apache.tika.io.LookaheadInputStream*'
case "$stage" in
  build)
    "$maven" "${common[@]}" "$@" -pl tika-core -am install -DskipTests
    ;;
  generate)
    for method in close markSupported read; do
      "$maven" -B -ntp "$@" -f tika-core/pom.xml -Ptache2-generation \
        "-DselectMethod=org.apache.tika.io.LookaheadInputStream#$method" \
        io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1:method
    done
    ;;
  test|test-original|test-generated|test-final)
    suite="${stage#test-}"
    [[ "$suite" == test ]] && suite=final
    selector='*'
    if [[ "$suite" == original ]]; then
      selector='*,!LookaheadInputStream_*_Test,!LookaheadInputStreamManualTest'
    elif [[ "$suite" == generated ]]; then
      selector='*,!LookaheadInputStreamManualTest'
    fi
    rm -f tika-core/target/jacoco.exec tika-core/target/surefire-reports/*.xml tika-core/target/surefire-reports/*.txt
    "$maven" "${common[@]}" "$@" "-Dtest=$selector" -Dsurefire.failIfNoSpecifiedTests=false -pl tika-core -am test | tee "evidence/$suite-suite.log"
    mkdir -p "evidence/coverage-$suite" "evidence/surefire-$suite"
    cp -R tika-core/target/site/jacoco/. "evidence/coverage-$suite/"
    cp tika-core/target/surefire-reports/* "evidence/surefire-$suite/"
    ;;
  original|generated|final)
    tests="$original"
    if [[ "$stage" == generated ]]; then
      tests="$generated"
    elif [[ "$stage" == final ]]; then
      tests='org.apache.tika.io.LookaheadInputStream*'
    fi
    extra=()
    if [[ "$stage" == generated ]]; then
      extra+=(-Dtache2.excludedTestClasses=org.apache.tika.io.LookaheadInputStreamManualTest)
    elif [[ "$stage" == final ]]; then
      extra+=(-Dtache2.mutationThreshold=100)
    fi
    "$maven" -B -ntp "$@" -f tika-core/pom.xml -Ptache2-mutation \
      "-Dtache2.targetTests=$tests" "${extra[@]}" \
      "-Dtache2.reportDirectory=$PWD/evidence/pit-$stage" \
      org.pitest:pitest-maven:mutationCoverage
    ;;
  all)
    "$0" build "$@"
    for suite in original generated final; do
      "$0" "test-$suite" "$@"
      "$0" "$suite" "$@" | tee "evidence/pit-$suite.log"
    done
    ;;
  *)
    echo 'Usage: scripts/tache2.sh {build|generate|test|original|generated|final|all} [Maven options]' >&2
    exit 2
    ;;
esac
