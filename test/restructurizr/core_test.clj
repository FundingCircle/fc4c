(ns restructurizr.core-test
  (:require [restructurizr.core :as rc]
            [clojure.test :as ct :refer [deftest]]
            [restructurizr.test-utils :as rtu :refer [check']]
            [clojure.spec.test.alpha :as st :refer [check]]))

(deftest blank-nil-or-empty? (check' (check `rc/blank-nil-or-empty?)))
(deftest parse-coords (check' (check `rc/parse-coords)))
(deftest round-to-closest (check' (check `rc/round-to-closest)))
(deftest snap-coords (check' (check `rc/snap-coords)))
(deftest shrink (check' (check `rc/shrink)))

;; This is a fake test that exists just to shut down the thread pools that back agents. None of the
;; code in this project uses agents, but I guess maybe one of the libraries does. Not sure which.
;; For more see https://groups.google.com/d/msg/clojure/zNh8zQbTQq4/VUjl75dwAgAJ
(deftest foo (do (shutdown-agents) (ct/is true)))
