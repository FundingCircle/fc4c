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
