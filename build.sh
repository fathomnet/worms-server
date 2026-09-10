#!/usr/bin/env bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

echo "--- Building worms-server (reminder: run docker login first!!)"

VCS_REF=`git tag | sort -V | tail -1`

sbt "Docker / stage" \
    && cd $SCRIPT_DIR/target/out/jvm/scala-3.9.0/worms-server/docker/stage \
    && docker buildx build --platform linux/amd64,linux/arm64 \
        -t mbari/worms-server:${VCS_REF} \
        -t mbari/worms-server:latest \
        --push . \
    && docker pull mbari/worms-server:latest


# podman run --name worms -p 8888:8080 -v "/home/podman_user/worms":"/opt/worms" mbari/worms-server