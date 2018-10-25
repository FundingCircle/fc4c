(ns fc4.integrations.structurizr.express.render-test
  (:require [fc4.integrations.structurizr.express.render :as r]
            [clojure.java.io                             :as io :refer [file input-stream]]
            [clojure.spec.alpha                          :as s]
            [clojure.test                                       :refer [deftest testing is]])
  (:import  [java.io DataInputStream]
            [java.util Arrays]))

(defn file-to-byte-array
  "Copied from https://stackoverflow.com/a/29640320/7012"
  [^java.io.File file]
  (let [result (byte-array (.length file))]
    (with-open [in (DataInputStream. (input-stream file))]
      (.readFully in result))
    result))

(deftest render
  (testing "happy paths"
    (testing "rendering a Structurizr Express file"
      (let [dir "test/data/structurizr/express/"
            yaml (slurp (str dir "diagram_valid_cleaned.yaml"))
            {:keys [::r/png-bytes ::r/stderr] :as result} (r/render yaml)
            expected-file (file (str dir "diagram_valid_cleaned.png"))
            expected-bytes (file-to-byte-array expected-file)]

        (is (s/valid? ::r/result result) (s/explain-str ::r/result result))

        (is (Arrays/equals png-bytes expected-bytes)
            (let [binary-spit (fn [path data]
                                (with-open [out (io/output-stream (file path))]
                                  (.write out data)))
                  actual-file-path (str dir "diagram_valid_cleaned_actual.png")]
              (binary-spit actual-file-path png-bytes)
              (str stderr
                   "\nfile with “expected” PNG: " expected-file
                   "\nactual PNG written to " actual-file-path)))))))
