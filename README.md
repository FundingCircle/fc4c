# FC4C

A tool for reorganizing, restructuring, and reformatting
[FC4](https://fundingcircle.github.io/fc4-framework/) diagrams.


## Purpose

As explained in
[The Toolset](https://fundingcircle.github.io/fc4-framework/methodology/toolset.html) section of
[the FC4 Methodology](https://fundingcircle.github.io/fc4-framework/methodology/):

> This tool was created because when one uses Structurizr Express (SE) to position the elements of a diagram, SE regenerates the diagram source YAML in such a way that the YAML becomes noisy and the sorting can change. This makes the source harder to work with in a text editor and impossible to usefully diff from revision to revision — and without useful diffing it’s very difficult to do effective peer review.
>
> So FC4C processes the YAML: cleans it up, applies a stable sort to all properties, removes empty properties, etc — so as to ensure that the changes applied in each revision are very small and specific and all extraneous changes are filtered out. This will hopefully enable effective peer review of revisions to the diagrams.
>
> FC4C also “snaps” the elements and vertices in a diagram to a virtual grid.

## Setup

1. Install Clojure as per [this guide](https://clojure.org/guides/getting_started)
2. Clone this repo
3. `cd` into the repo
4. To install the dependencies, run: `clj -e '(println "deps installed!")'`

## Basic Usage

1. Have `clj` installed ([guide](https://clojure.org/guides/getting_started))
1. In your shell: `clj`
1. In the REPL:
   1. `(use 'fc4c.repl)`
   1. Read the printed descriptions of `pcb` and `wcb`
   1. Run either `(pcb)` or `(wcb)`

(There will soon be an option to run `wcb` directly from the command-line.)

## Full Usage Workflow

As explained in [The Authoring Workflow](https://fundingcircle.github.io/fc4-framework/methodology/authoring_workflow.html) section of
[the FC4 Methodology](https://fundingcircle.github.io/fc4-framework/methodology/):

> 1. In your text editor: either create a new diagram source file or open an existing diagram source file
> 1. In a terminal, in your FC4C working dir:
>    1. Open a Clojure REPL with `clj`
>    1. Evaluate `(use 'fc4c.repl')` then `(wcb)`
>    1. This starts a background process that will watch your clipboard for diagram source YAML and process (clean up) that YAML when it sees that it’s been changed.
> 1. In your text editor, add/revise elements and relationships, then select-all and cut the diagram source from your editor into your system clipboard.
>    1. This will cause FC4C to process the contents of your clipboard.
> 1. Switch to [Structurizr Express](https://structurizr.com/help/express) (SE) » paste the source into the YAML textarea » press tab to blur the textarea
>    1. SE will either render the diagram, or display a red error indicator in its toolbar
>    2. If SE shows its red error indicator, click the indicator button to bring up a dialog listing the errors
> 1. Use SE to arrange the elements and edges as desired
> 1. Cut the diagram source from the SE YAML textarea into your system clipboard.
>    1. This will cause FC4C to process the contents of your clipboard.
> 1. Paste the diagram source back into the SE YAML textarea so as to re-render the diagram, now that the elements have been “snapped” to a virtual grid.
> 1. Continue to cut and past the diagram source between your text editor and SE, using SE to preview and adjust the rendered diagram, while FC4C cleans up the diagram as you work.

## Running the Tests

1. Use CI
2. No, seriously, use CI!
3. Just kidding, I know sometimes you need to run the tests locally ;)

### With Docker

Run this in your shell:

```bash
docker run --rm `docker build -q .`
```

### Without Docker

If you’re old-school and prefer to run tests on bare metal:

1. Have `clj` installed ([guide](https://clojure.org/guides/getting_started))
1. Run in your shell: `clj -A:run-tests`

## Starting a REPL for Dev/Test

You could just run `clj` but you’re likely to want the test deps and code to be accessible. In that
case run `clj -Adev`

## Contributors

* @99-not-out
* @arrdem
* @matthias-margush
* @sgerrand
* @timgilbert

Thank you all!

(If you notice that anyone is missing from this list, please open an issue or a PR — thank you!)
