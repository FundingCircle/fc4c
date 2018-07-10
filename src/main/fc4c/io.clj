(ns fc4c.io
  "Provides all I/O facilities so that the other namespaces can be pure. The
  function specs are provided as a form of documentation and for instrumentation
  during development. They should not be used for generative testing."
  (:require [clojure.java.io         :as io]
            [clojure.spec.alpha      :as s]
            [clojure.spec.gen.alpha  :as gen]
            [clojure.string          :as str :refer [ends-with?]]
            [cognitect.anomalies     :as anom]
            [expound.alpha           :as expound :refer [expound-str]]
            [fc4c.files              :as files :refer [relativize]]
            [fc4c.model              :as m :refer [elements-from-file]]
            [fc4c.spec               :as fs]
            [fc4c.styles             :as st :refer [styles-from-file]]
            [fc4c.util               :as u :refer [lookup-table-by]]
            [fc4c.yaml               :as fy :refer [split-file]]
            [fc4c.view               :as v :refer [view-from-file]])
  (:import [java.io FileNotFoundException]))

(defn yaml-files
  "Accepts a directory as a path string or a java.io.File, returns a lazy sequence of java.io.File objects for
  all the YAML files in that dir or in any of its child dirs (recursively) to an unlimited depth."
  [dir]
  (->> (io/file dir)
       file-seq
       (filter #(or (ends-with? % ".yaml")
                    (ends-with? % ".yml")))))

(s/fdef yaml-files
        :args (s/cat :dir ::fs/dir-path)
        :ret  (s/coll-of (partial instance? java.io.File)))

(defn process-dir
  "Accepts a directory path as a string, finds all the YAML files in that dir or
  in any of its child dirs (recursively) to an unlimited depth, and applies f to
  the contents of each file, overwriting its current contents. Prints out the
  path of each file before processing it. If an error occurs, it is thrown
  immediately, aborting the work."
  [dir-path f]
  (doseq [file (yaml-files dir-path)]
    (binding [*out* *err*]
      (println (relativize (str file) dir-path)))
    (->> (slurp file)
         (f)
         (spit file))))

(s/fdef process-dir
        :args (s/cat :dir-path ::fs/dir-path
                     :f        (s/fspec :args (s/cat :file-contents string?)
                                        :ret  string?))
        :ret  nil?)

(defn- read-model-elements
  "Recursively find and read all elements from all YAML files under a directory
  tree."
  [root-path]
  (->> (yaml-files root-path)
       (map (juxt slurp identity))
       (mapcat (fn [[file-contents file-path]]
                 (-> (split-file file-contents)
                     (get ::fy/main)
                     (elements-from-file file-path root-path))))
       ((partial lookup-table-by ::m/name))))

(s/fdef read-model-elements
        :args (s/cat :root-path ::fs/dir-path)
        :ret  (s/map-of ::m/name ::m/element))

(s/def ::invalid-result any?)

(s/def ::error
  (s/merge ::anom/anomaly (s/keys :req [::invalid-result])))

(defn- validate-model-dirs
  "Validates that the root dir and the required child dirs exist and are
  actually dirs. If anything is invalid, throws a FileNotFoundException or a
  RuntimeException. Otherwise returns nil."
  [root-path]
  (let [d (partial io/file root-path)]
    (doseq [dir [(d) (d "systems") (d "users")]]
      (when-not (.exists dir)
        (throw (FileNotFoundException.
                (str "The directory " dir " does not exist."))))
      (when-not (.isDirectory dir)
        (throw (RuntimeException.
                (str "The path " dir " is not a directory.")))))))

(defn- val-or-error
  [v spec]
  (if (s/valid? spec v)
    v
    {::anom/category ::anom/fault
     ::anom/message (expound-str spec v)
     ::invalid-result v}))

(defn read-model
  "Pass the path of a dir that contains the dirs \"systems\" and \"users\"."
  [root-path]
  (validate-model-dirs root-path)
  (let [model {::m/systems (read-model-elements (io/file root-path "systems"))
               ::m/users (read-model-elements (io/file root-path "users"))}]
    (val-or-error model ::m/model)))

(s/fdef read-model
        :args (s/cat :root-path ::fs/dir-path)
        :ret  (s/or :success ::m/model
                    :error   ::error))

(defn read-view
  [file-path]
  (-> (slurp file-path)
      (split-file)
      (get ::fy/main)
      (view-from-file)
      (val-or-error ::v/view)))

(s/fdef read-view
        :args (s/cat :file-path ::fs/file-path-str)
        :ret  (s/or :success ::v/view
                    :error   ::error))

(defn read-styles
  [file-path]
  (-> (slurp file-path)
      (split-file)
      (get ::fy/main)
      (st/styles-from-file)
      (val-or-error ::st/styles)))

(s/fdef read-styles
        :args (s/cat :file-path ::fs/file-path-str)
        :ret  (s/or :success ::st/styles
                    :error   ::error))