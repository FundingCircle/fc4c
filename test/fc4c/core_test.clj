(ns fc4c.core-test
  (:require [fc4c.core :as rc]
            [clojure.test :as ct :refer [deftest]]
            [fc4c.test-utils :as rtu :refer [check]]
            [clojure.spec.test.alpha :as st]))

(deftest blank-nil-or-empty? (check `rc/blank-nil-or-empty?))
(deftest parse-coords (check `rc/parse-coords))
(deftest round-to-closest (check `rc/round-to-closest))
(deftest snap-coords (check `rc/snap-coords))
(deftest shrink (check `rc/shrink 300))

;; This is a fake test that exists just to shut down the thread pools that back agents. None of the
;; code in this project uses agents, but I guess maybe one of the libraries does. Not sure which.
;; For more see https://groups.google.com/d/msg/clojure/zNh8zQbTQq4/VUjl75dwAgAJ
(deftest foo (do (shutdown-agents) (ct/is true)))
