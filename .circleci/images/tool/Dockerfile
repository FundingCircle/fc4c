# I wish the “official” Node images included a tag to fix the version of Alpine
# used, but they don’t. As of this writing in late October 2018, the base image
# uses Alpine 3.8.
FROM node:10.12-alpine

LABEL maintainer="Avi Flax <avi.flax@fundingcircle.com>"
LABEL description="Node, Clojure, and Chromium on Alpine"

ENV JRE_VERSION=8.181.13-r0
ENV CLOJURE_VERSION=1.9.0.394

WORKDIR /tmp

RUN apk update

# Dependencies of Clojure.
# NB: the OpenJDK image published by Docker the company does a few things beyond
# just apk adding the openjdk-jre package; those things might also be necessary;
# not sure. See
# https://github.com/docker-library/openjdk/blob/master/8/jre/alpine/Dockerfile
RUN apk -q --no-progress add --no-cache openjdk8-jre=$JRE_VERSION bash curl

# Clojure itself
RUN wget -q https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh \
      && chmod +x linux-install-$CLOJURE_VERSION.sh \
      && ./linux-install-$CLOJURE_VERSION.sh

# Add the dependencies of the Chromium that Puppeteer downloads for its own use.
# We don’t actually need this Chromium, only its dependencies. Hypothetically we
# could `apk del chromium` after installing it all, in order to reduce the image
# size, but that also deletes all the dependencies that would then be unneeded,
# as far as apk knows. So maybe we’ll figure that out at some point. Actually I
# think rm -Rf /usr/lib/chromium should work, but I had some trouble with that.
# I’m not sure why; it could be my lack of experience working with remote Docker
# repositories. Might be worth trying again at some point.
RUN apk -q --no-progress add --no-cache udev ttf-freefont libc6-compat chromium
