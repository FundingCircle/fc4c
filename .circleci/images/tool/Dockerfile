# I wish the “official” Node images included a tag to fix the version of Alpine
# used, but they don’t. Therefore we’re specifing a specific image hash to use
# rather than a tag.
# This base image at this hash uses Node 10.12 and Alpine 3.8:
FROM node@sha256:1e3e3e7ffc965511c5d4f4e90ec5d9cabee95b5b1fbcd49eb6a2289f425cf183

LABEL maintainer="Avi Flax <avi.flax@fundingcircle.com>"
LABEL description="Node, Clojure, and Chromium on Alpine"

ENV JRE_VERSION=8.181.13-r0
ENV CLOJURE_VERSION=1.9.0.394

WORKDIR /tmp

# Dependencies of Clojure and the Clojure installer.
# NB: the OpenJDK image published by Docker the company does a few things beyond
# just apk adding the openjdk-jre package; those things might also be necessary;
# not sure. See
# https://github.com/docker-library/openjdk/blob/master/8/jre/alpine/Dockerfile
RUN apk -q --no-progress --no-cache add openjdk8-jre=$JRE_VERSION bash curl

# Clojure itself
RUN wget -q https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh \
      && chmod +x linux-install-$CLOJURE_VERSION.sh \
      && ./linux-install-$CLOJURE_VERSION.sh

# We need Chromium for automated rendering with Puppeteer; see
#   https://github.com/GoogleChrome/puppeteer/blob/master/docs/troubleshooting.md#running-on-alpine
# This specific version of Chromium is locked to the specific version of
# Puppeteer that we’re using as per the above page; see also
#   <project-root>/tool/renderer/package.json
RUN apk -q --no-progress --no-cache add chromium=68.0.3440.75-r0
