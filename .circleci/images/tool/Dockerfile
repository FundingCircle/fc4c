# I wish the “official” Node images included a tag to fix the version of Alpine
# used, but they don’t. As of this writing in late October 2018, the base image
# uses Alpine 3.8.
FROM node:10.12-alpine

LABEL maintainer="Avi Flax <avi.flax@fundingcircle.com>"
LABEL description="Node and Clojure on Alpine."

ENV JRE_VERSION=8.181.13-r0
ENV CLOJURE_VERSION=1.9.0.394

WORKDIR /tmp

# Dependencies of Clojure.
#
# We might not really need rlwrap because it’s really only needed for the REPL,
# but including it anyway just in case we need to use the REPL in the container.
#
# OTOH, I just noticed this:
# https://docs.docker.com/develop/develop-images/dockerfile_best-practices/#dont-install-unnecessary-packages
# which says:
#
# > To reduce complexity, dependencies, file sizes, and build times, avoid
# > installing extra or unnecessary packages just because they might be “nice to
# > have.” For example, you don’t need to include a text editor in a database
# > image.
#
# ...so. Hmm.
#
# NB: the OpenJDK image published by Docker the company does a few things beyond
# just apk adding the openjdk-jre package; those things might also be necessary;
# not sure. See
# https://github.com/docker-library/openjdk/blob/master/8/jre/alpine/Dockerfile
# RUN echo '@testing http://dl-cdn.alpinelinux.org/alpine/edge/testing' >> /etc/apk/repositories
RUN apk add --update --no-cache openjdk8-jre=$JRE_VERSION bash curl
# rlwrap@testing

# Clojure itself
RUN wget https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh \
      && chmod +x linux-install-$CLOJURE_VERSION.sh \
      && ./linux-install-$CLOJURE_VERSION.sh
