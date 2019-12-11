(ns fc4.model.dsl
  (:require [clj-yaml.core :as yaml]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :refer [includes? join starts-with?]]
            [cognitect.anomalies :as anom]
            [expound.alpha :as expound :refer [expound-str]]
            [fc4.model :as m]
            [fc4.util :as u :refer [add-ns fault fault? update-all]]
            [fc4.yaml :as fy :refer [split-file]]
            [medley.core :refer [deep-merge map-vals]])
  (:import [org.yaml.snakeyaml.parser ParserException]))

(defn- postprocess-keys
  "First qualify each keyword key using the fc4.model namespace. Then check if a corresponding spec
  exists for the resulting qualified keyword. If it does, then replace the key with the qualified
  key. If it does not, then use the string version of the keyword, because it’s not a “keyword” of
  the DSL, so it’s probably a name or a tag name (key)."
  [m]
  (update-all
   (fn [[k v]]
     (let [qualified (add-ns "fc4.model" k)]
       (if (s/get-spec qualified)
         [qualified v]
         [(name k) v])))
   m))

(defn parse-model-file
  "Given a YAML model file as a string, parses it, and qualifies all map keys
  except those at the root so that the result has a chance of being a valid
  ::file-map. If a file contains “top matter” then only the main document is
  parsed. Performs very minimal validation. If the file contains malformed YAML,
  or does not contain a map, an anomaly will be returned."
  [file-contents]
  (try
    (let [parsed (-> (split-file file-contents)
                     (::fy/main)
                     (yaml/parse-string))]
      (if (associative? parsed)
        (map-vals postprocess-keys parsed)
        (fault "Root data structure must be a map (mapping).")))
    (catch ParserException e
      (fault (str "YAML could not be parsed: error " e)))))

(s/fdef parse-model-file
  :args (s/cat :v (s/alt :valid-and-well-formed ::file-map-yaml-string
                         :invalid-or-malformed  string?))
  :ret  (s/or :valid-and-well-formed ::file-map
              :invalid-or-malformed  ::anom/anomaly)
  :fn   (fn [{{arg :v} :args, ret :ret}]
          (= (first arg) (first ret))))

(defn validate-parsed-file
  "Returns either an error message as a string or nil."
  [parsed]
  (cond
    (s/valid? ::file-map parsed)
    nil

    (fault? parsed)
    (::anom/message parsed)

    :else
    (expound-str ::file-map parsed)))

(s/fdef validate-parsed-file
  :args (s/cat :parsed (s/alt :valid   ::file-map
                              :invalid map?))
  :ret  (s/or                 :valid   nil?
                              :invalid string?)
  :fn   (fn [{{arg :parsed} :args, ret :ret}]
          (= (first arg) (first ret))))

(def ^:private dsl-to-model-maps
  {:systems    ::m/systems
   :users      ::m/users
   :datastores ::m/datastores})

(defn add-file-map
  "Adds the elements from a parsed DSL file to a model. If any of the elements in the file-map are
  already in the model, they’re merged (using medley/deep-merge) because the model DSL supports
  breaking the definition of a (presumably large) system across multiple files."
  [model file-map]
  (reduce
   (fn [model [src dest]]
     (if-let [src-map (get file-map src)]
       (update model dest deep-merge src-map)
       model))
   model
   dsl-to-model-maps))

(defn ^:private contains-contents?
  "Given a model (or proto-model) and the contents of a parsed model DSL yaml
  file, a ::file-map, returns true if the model contains all the contents of
  the file-map."
  [model file-map]
  ;; Ideally the below would validate *fully* that each element in the file is fully contained in
  ;; the file. However, because an element can be defined in multiple files and therefore the
  ;; resulting element in the model is a composite (a result of deeply merging the various
  ;; definitions) I don’t know how to validate this. I guess I’m just not smart enough. I mean, I
  ;; suspect I could figure it out eventually given enough time — it’d probably have to do with
  ;; depth-first walking the file element and then confirming that the model contains the same value
  ;; at the same path. But I don’t have the time or energy to figure that out right now.
  ;; TODO: figure this out.
  (->> (for [[src dest] dsl-to-model-maps]
         (for [[file-elem-name _file-elem-val] (get file-map src)]
           (contains? (get model dest) file-elem-name)))
       (flatten)
       (every? true?)))

(s/fdef add-file-map
  :args (s/cat :pmodel   ::m/proto-model
               :file-map ::file-map)
  :ret  (s/or :success ::m/proto-model
              :failure ::anom/anomaly)
  :fn   (fn [{{:keys [_pmodel file-map]} :args
              [ret-tag ret-val]          :ret}]
          (and
           ; The :ret spec allows the return value to be either a proto-model
           ; or a valid anomaly. If a value is passed to it that is actually
           ; both a valid proto-model *and* a valid anomaly, it will be
           ; considered valid by the spec, and will be tagged as a :success,
           ; but only because :success is the first spec in the or. So let’s
           ; just ensure this doesn’t happen.
           (not (and (s/valid? ::m/proto-model ret-val)
                     (s/valid? ::anom/anomaly  ret-val)))
           (case ret-tag
             ;; TODO: also validate that ret-val contains the contents of pmodel.
             :success
             (contains-contents? ret-val file-map)

             ;; TODO: also validate that the inputs do indeed have duplicate keys
             :failure
             (includes? (or (::anom/message ret-val) "") "duplicate names")))))

(defn build-model
  "Accepts a sequence of maps read from model YAML files and combines them into
  a single model map. If any name collisions are detected then an anomaly is
  returned. Does not validate the result."
  [file-maps]
  (reduce
   (fn [model file-map]
     (let [result (add-file-map model file-map)]
       (if (fault? result)
         (reduced result)
         result)))
   (m/empty-model)
   file-maps))

(s/fdef build-model
  :args (s/cat :in (s/coll-of ::file-map))
  :ret  (s/or :success ::m/proto-model
              :failure ::anom/anomaly)
  :fn   (fn [{{:keys [in]}      :args
              [ret-tag ret-val] :ret}]
          (and
           ; I saw, at least once, a case wherein the return value was  both a
           ; valid proto-model *and* a valid anomaly. We don’t want this.
           (not (and (s/valid? ::m/proto-model ret-val)
                     (s/valid? ::anom/anomaly  ret-val)))
           (case ret-tag
             :success
             (every? #(contains-contents? ret-val %) in)

             :failure
             (includes? (or (::anom/message ret-val) "") "duplicate names")))))
