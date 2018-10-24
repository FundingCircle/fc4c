# fc4-tool

A tool for reorganizing, restructuring, reformatting, and rendering
[FC4](https://fundingcircle.github.io/fc4-framework/) diagrams.

[![CircleCI](https://circleci.com/gh/FundingCircle/fc4-framework.svg?style=shield)](https://circleci.com/gh/FundingCircle/fc4-framework)
[![codecov](https://codecov.io/gh/FundingCircle/fc4-framework/branch/master/graph/badge.svg)](https://codecov.io/gh/FundingCircle/fc4-framework)

## Purpose

As explained in
[The Toolset](https://fundingcircle.github.io/fc4-framework/methodology/toolset.html) section of
[the FC4 Methodology](https://fundingcircle.github.io/fc4-framework/methodology/):

> This tool was created because when one uses Structurizr Express (SE) to position the elements of a diagram, SE regenerates the diagram source YAML in such a way that the YAML becomes noisy and the sorting can change. This makes the source harder to work with in a text editor and impossible to usefully diff from revision to revision — and without useful diffing it’s very difficult to do effective peer review.
>
> So fc4-tool processes the YAML: cleans it up, applies a stable sort to all properties, removes empty properties, etc — so as to ensure that the changes applied in each revision are very small and specific and all extraneous changes are filtered out. This will hopefully enable effective peer review of revisions to the diagrams.
>
> fc4-tool also:
>
> * “Snaps” the elements and vertices in a diagram to a virtual grid
> * Renders diagrams

## Setup

### Quick Setup on Mac OS X

1. Make sure [Homebrew](https://brew.sh/) is installed (try running `brew`)
1. Clone [this repo](https://github.com/FundingCircle/fc4-framework) and `cd` into it
1. Run: `brew install clojure node && cd tool && bin/download-all-deps`

### Manual Setup

1. Install Clojure as per [this guide](https://clojure.org/guides/getting_started)
   1. This project uses the new Clojure CLI (`clj`) and
      [tools.deps](https://clojure.org/guides/deps_and_cli), both of which are installed by
      [the new official Clojure installers](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)
      released alongside Clojure 1.9. If you’ve been working with Clojure for awhile, you might
      not have these tools installed. Try `which clj` to check, and if that prints a blank line,
      try running the appropriate
      [installer](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools).
1. Install [Node.js](https://nodejs.org/) (used for rendering diagrams)
1. Clone [this repo](https://github.com/FundingCircle/fc4-framework)
1. `cd` into the repo and then `cd tool`
1. To install the dependencies run `bin/download-all-deps`

## Basic Usage

### Editing and Rendering Diagrams

1. Run in your shell, from the root of the repo: `cd tool && ./fc4 wcb`
1. Copy-and-paste YAML diagram definitions between [Structurizr Express](https://structurizr.com/help/express) (SE) and an open file in your text editor.
1. When done, ensure the YAML in your editor is the latest version, copy-and-pasting from SE one last time if necessary, then save the file.
1. Switch to your terminal and hit ctrl-c to stop `fc4 wcb`
1. Run `./fc4 render <path-to-yaml-file>` to generate a `.png` file alongside the `.yaml` file
1. Commit both files

## Full Usage Workflow

Please see [The Authoring Workflow](https://fundingcircle.github.io/fc4-framework/methodology/authoring_workflow.html) section of
[the FC4 Methodology](https://fundingcircle.github.io/fc4-framework/methodology/).

## Running the Tests

1. Use CI
2. No, seriously, use CI!
3. Just kidding, I know sometimes you need to run the tests locally ;)

### With Docker

Run this in your shell:

```bash
bin/run bin/tests
```

### Without Docker

If you’re old-school and prefer to run tests on bare metal:

1. Have all the dependencies installed as per [Setup](#setup)
1. Run in your shell: `bin/tests`

## Starting a REPL for Dev/Test

You _could_ just run `clj` but you’re likely to want the test deps and dev utils to be accessible.
So you’ll probably want to run `clj -A:dev:test`

### Running the tests in a REPL

```
$ clj -A:dev:test
Clojure 1.9.0
user=> (require '[eftest.runner :refer [find-tests run-tests]])
user=> (run-tests (find-tests "test") {:fail-fast? true})
...
```

## Running the Linter

For linting, this project uses [cljfmt](https://github.com/weavejester/cljfmt),
via [cljfmt-runner](https://github.com/JamesLaverack/cljfmt-runner).

* To lint the entire project, run `clojure -A:lint`
* To lint the entire project **and automatically fix any problems found** run
  `clojure -A:lint:lint/fix`
  * This will change the files on disk but will not commit the changes nor stage
    them into the git index. This way you can review the changes that were
    applied and decide which to keep and which to discard.

## Contributors

* [99-not-out](https://github.com/99-not-out)
* [arrdem](https://github.com/arrdem)
* [matthias-margush](https://github.com/matthias-margush)
* [sgerrand](https://github.com/sgerrand)
* [sldblog](https://github.com/sldblog)
* [timgilbert](https://github.com/timgilbert)

Thank you all!

(If you notice that anyone is missing from this list, please open an issue or a PR — thank you!)
