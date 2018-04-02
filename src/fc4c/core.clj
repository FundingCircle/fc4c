#!/usr/local/bin/clojure

(ns fc4c.core
  (:require [fc4c.spec :as fs]
            [clj-yaml.core :as yaml]
            [clojure.spec.alpha :as s]
            [com.gfredericks.test.chuck.generators :as gen']
            [flatland.ordered.map :refer [ordered-map]]
            [clojure.string :as str :refer [blank? join split trim]]
            [clojure.walk :as walk :refer [postwalk]]
            [clojure.set :refer [difference intersection]]))

(def default-front-matter
  (str "links:\n"
       "  The FC4 Framework: https://fundingcircle.github.io/fc4-framework/\n"
       "  Structurizr Express: https://structurizr.com/express"))

(defn split-file
  "Accepts a string containing either a single YAML document, or a YAML document
  and front matter (which itself is a YAML document). Returns a seq like
  [front? main] wherein front? may be nil if the input string does not contain
  front matter, or does not contain a valid separator. In that case main may or
  may not be a valid YAML document, depending on how mangled the document
  separator was."
  [s]
  (let [matcher (re-matcher #"(?ms)((?<front>.+)---\n)?(?<main>.+)\Z" s)
        _ (.find matcher)
        front (.group matcher "front")
        main (.group matcher "main")]
    [front main]))

(defn blank-nil-or-empty? [v]
  (or (nil? v)
      (and (coll? v)
           (empty? v))
      (and (string? v)
           (blank? v))))

(s/fdef blank-nil-or-empty?
  :args (s/cat :v (s/or :nil nil?                        
                        :coll (s/coll-of any?
                                         ; for this fn it matters only if the coll is empty or not
                                         :gen-max 1)
                        :string string?))
  :ret boolean?
  :fn (fn [{:keys [args ret]}]
        (let [[which v] (:v args)]
          (case which
            :nil
            (= ret true)
            
            :coll
            (if (empty? v)
                (= ret true)
                (= ret false))

            :string
            (if (blank? v)
                (= ret true)
                (= ret false))))))

(defn shrink
  "Remove key-value pairs wherein the value is blank, nil, or empty from a
  (possibly nested) map. Also transforms maps to nil if all of their values are
  nil, blank, or empty.
  
  Adapted from https://stackoverflow.com/a/29363255/7012"
  [in]
  (postwalk (fn [el]
              (if (map? el)
                  (let [m (into (empty el) ; using empty to preserve ordered maps 
                            (remove (comp blank-nil-or-empty? second) el))]
                    (when (seq m) m))
                  el))
            in))

(s/fdef shrink
  :args (s/cat :in :fc4c/diagram)
  :ret :fc4c/diagram
  :fn (s/and
        (fn [{{in :in} :args, ret :ret}] (= (type in) (type ret)))
        (fn [{{in :in} :args, ret :ret}]
          (let [all-vals #(if (map? %) (vals %) %) ; works on maps or sequences
                leaf-vals #(->> (tree-seq coll? all-vals %)
                                (filter (complement coll?))
                                flatten)
                in-vals (->> (leaf-vals in) (filter (complement blank-nil-or-empty?)) set)
                ret-vals (->> (leaf-vals ret) set)]
            (if (seq in)
                (= in-vals ret-vals)
                (nil? ret))))))

(defn reorder
  "Reorder a map as per a seq of keys.
  
  Accepts a seq of keys and a map; returns a new ordered map containing the
  specified keys and their corresponding values from the input map, in the same
  order as the specified keys. If any keys present in the input map are omitted
  from the seq of keys, the corresponding k/v pairs will be sorted “naturally”
  after the specified k/v pairs."
  [ks m]
  (let [specified-keys (set ks) ; reminder: this set is unordered.
        present-keys (set (keys m)) ; reminder: this set is unordered.
        unspecified-but-present-keys (difference present-keys specified-keys)
        ; The below starts with ks because the above sets don’t retain order. I
        ; tried using flatland.ordered.set but the difference and intersection
        ; functions from clojure.set did not work as expected with those. This
        ; means this function won’t filter out keys that are specified but not
        ; present, and therefore those keys will be present in the output map with
        ; nil values. This is acceptable to me; I can work with it.
        all-keys-in-order (concat ks (sort unspecified-but-present-keys))]
    (into (ordered-map)
          (map (juxt identity (partial get m))
               all-keys-in-order))))

(def desired-order
  {:root          {:sort-keys nil
                   :key-order [:type :scope :description :elements
                               :relationships :styles :size]}
   :elements      {:sort-keys [:type :name]
                   :key-order [:type :name :description :tags :position
                               :containers]}
   :relationships {:sort-keys [:order :source :destination]
                   :key-order [:order :source :description :destination
                               :technology :vertices]}
   :styles        {:sort-keys [:type :tag]
                   :key-order [:type :tag]}})

(defn reorder-diagram
  "Apply desired order/sort to diagram keys and values.
  
  Accepts a diagram as a map. Returns the same map with custom ordering/sorting applied to the
  root-level key-value pairs and many of the nested sequences of key-value
  pairs as per desired-order."
  [diagram]
  (reduce
    (fn [d [key {:keys [sort-keys key-order]}]]
      (if (= key :root)
          (reorder key-order d)
          (update-in d [key]
            #(->> (sort-by (comp join (apply juxt sort-keys)) %)
                  (map (partial reorder key-order))))))
    diagram
    desired-order))

(def coord-pattern (re-pattern (str "^" fs/coord-pattern-base "$")))

(defn parse-coords [s]
  (some->> s
           (re-find coord-pattern)
           (drop 1)
           (map #(Integer/parseInt %))))

(s/fdef parse-coords
  :args (s/cat :s :fc4c/coord-string)
  :ret (s/coll-of :fc4c/coord-int :count 2)
  :fn (fn [{:keys [ret args]}]
        (= ret
           (->> (split (:s args) #",") 
                (map trim)
                (map #(Integer/parseInt %))))))

(defn round-to-closest [target n]
  (if (zero? n)
      0
      (-> (/ n (float target))
          Math/round
          (* target))))

(s/def ::snap-target #{10 25 50 75 100})

(s/fdef round-to-closest
  :args (s/cat :target ::snap-target
               :n :fc4c/coord-int)
  :ret :fc4c/coord-int
  :fn (fn [{{:keys [target n]} :args
            ret :ret}]
        (if (zero? ret) ;;TODO: need to actually validate that the ret value should actually be 0
            true
            (zero? (rem ret target)))))

(def elem-offsets
  {"Person" [25, -50]})

(defn snap-coords
  "Accepts a seq of X and Y numbers, and config values and returns a string in
  the form \"x,y\"."
  ([coords to-closest min-margin]
   (snap-coords coords to-closest min-margin (repeat 0)))
  ([coords to-closest min-margin offsets]
   (->> coords
        (map (partial round-to-closest to-closest))
        (map (partial max min-margin)) ; minimum left/top margins
        (map + offsets)
        (join ","))))

(s/fdef snap-coords
  :args (s/cat :coords (s/coll-of nat-int? :count 2)
               :to-closest nat-int?
               :min-margin nat-int?)
  :ret :fc4c/coord-string
  :fn (fn [{:keys [ret args]}]
        (let [parsed-ret (parse-coords ret)
              {:keys [:to-closest :min-margin]} args]
         (every? #(>= % min-margin) parsed-ret))))

(defn snap-elem-to-grid
  "Accepts an element (a software system, person, container, or
  component) as a map and snaps its position (coords) to a grid using the
  specified values."
  [elem to-closest min-margin]
  (let [coords (parse-coords (:fc4c/position elem))
        offsets (get elem-offsets (:fc4c.element/type elem) (repeat 0))
        new-coords (snap-coords coords to-closest min-margin offsets)]
    (assoc elem :fc4c.element/position new-coords)))

(s/fdef snap-elem-to-grid
  :args (s/cat :elem :fc4c/element
               :to-closest ::snap-target
               :min-margin nat-int?)
  :ret :fc4c/element
  :fn (fn [{{:keys [elem to-closest min-margin]} :args, ret :ret}]
        (= (:fc4c/position ret)
           (-> (:fc4c/position elem)
               parse-coords
               (snap-coords to-closest min-margin)))))

(defn snap-vertices-to-grid
  "Accepts an ordered-map representing a relationship, and snaps its vertices, if any, to a grid
  using the specified values."
  [e to-closest min-margin]
  (assoc e :vertices
    (map #(snap-coords (parse-coords %) to-closest min-margin)
         (:vertices e))))

(def elem-types
  #{"Person" "Software System" "Container" "Component"})

(defn snap-to-grid
  "Accepts a diagram as a map, a grid-size number, and a min-margin number. Searches the doc
  for elements and adjusts their positions so as to effectively “snap” them to a virtual grid of
  the specified size, and to ensure that each coord is no “smaller” than the min-margin number.
  
  Accounts for a quirk of Structurizr Express wherein elements of type “Person” need to be offset
  from other elements in order to align properly with them."
  [d to-closest min-margin]
  (postwalk
    #(cond
       (and (contains? elem-types (:type %))
             ; Checking for :position alone wouldn’t be sufficient; relationships can also have it
             ; and it means something different for them.
            (:position %))
       (snap-elem-to-grid % to-closest min-margin)
 
       (:vertices %)
       (snap-vertices-to-grid % (/ to-closest 2) min-margin)
 
       :else
       %)
    d))

(defn fixup-yaml
  "Accepts a diagram as a YAML string and applies some custom formatting rules."
  [s]
  (-> s
    (str/replace #"(\d+,\d+)" "'$1'")
    (str/replace #"(elements|relationships|styles|size):" "\n$1:")
    (str/replace #"(description): Uses\n" "$1: uses\n")))

(defn process
  "Accepts a diagram as a map; reorders everything, snaps all coordinates to a
  virtual grid, and removes all empty/blank nodes."
  [d]
  (-> (reorder-diagram d)
      (snap-to-grid 100 50)
      shrink)) ; must follow reorder-diagram because that tends to introduce new keys with nil values

(s/fdef process
  :args (s/cat :in :fc4c/diagram)
  :ret :fc4c/diagram)

(defn stringify
  "Accepts a diagram as a map, converts it to a YAML string."
  [d]
  (-> (yaml/generate-string d :dumper-options {:flow-style :block})
      fixup-yaml))

(defn process-file
  "Accepts a string containing either a single YAML document, or a YAML document and front matter
  (which itself is a YAML document). Returns a seq containing in the first position the fully
  processed main document as an ordered-map, and in the second a string containing first some front
  matter, the front matter separator, and then the fully processed main document."
  [s]
  (let [[front? main] (split-file s)
        main-processed (-> main
                           yaml/parse-string
                           process)
        str-output (str (or front? default-front-matter)
                        "\n---\n"
                        (stringify main-processed))]
    [main-processed str-output]))

(defn -main []
  (-> (slurp *in*)
      process-file
      second
      print)
  (flush)
  (Thread/sleep 10))
