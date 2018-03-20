(ns fc4c.test-utils
  (:require [clojure.test :as t :refer [is]]
            [clojure.spec.test.alpha :as st]
            [clojure.pprint :as pprint]))

;; Utility functions to integrate clojure.spec.test/check with clojure.test, copied from
;;   https://gist.github.com/Risto-Stevcev/dc628109abd840c7553de1c5d7d55608

(defn summarize-results'
  "Copied from https://gist.github.com/Risto-Stevcev/dc628109abd840c7553de1c5d7d55608"
  [spec-check]
  (map (comp #(pprint/write % :stream nil) st/abbrev-result) spec-check))

(defn check'
  "Copied from https://gist.github.com/Risto-Stevcev/dc628109abd840c7553de1c5d7d55608"
  [spec-check]
  (is (nil? (-> spec-check first :failure)) (summarize-results' spec-check)))

(defn opts [num-tests] {:clojure.spec.test.check/opts {:num-tests num-tests}})

(defn check
  ([sym] (check sym 1000))
  ([sym num-tests]
   (check'
     (st/check sym (opts num-tests)))))
