# FC4C

A tool for reorganizing, restructuring, and reformatting [FC4](https://fundingcircle.github.io/fc4-framework/) diagrams.

## Basic Usage

1. Have `clj` installed ([guide](https://clojure.org/guides/getting_started))
1. In your shell: `clj`
1. In the REPL:
   1. `(use 'fc4c.repl)`
   1. Read the printed descriptions of `pcb` and `wcb`
   1. Run either `(pcb)` or `(wcb)`

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
1. Run in your shell: `clj -Atest`

## Starting a REPL for Dev/Test

You could just run `clj` but you’re likely to want the test deps and code to be accessible. In that
case run `clj -Ctest -Rtest`
