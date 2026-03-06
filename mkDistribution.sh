#!/usr/bin/env bash
export BRANCH=draft
export NAME_BASE='universal-verifier'
git archive --format=zip $BRANCH \
  src test-data pom.xml README.md \
  > Polyas3.0-$NAME_BASE-`date "+%Y.%m.%d%n"`-`git describe $BRANCH --always`.zip