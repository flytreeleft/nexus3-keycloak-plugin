#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

pushd "${DIR}"
    mvn -PbuildKar clean package -Dmaven.test.skip=true
popd
