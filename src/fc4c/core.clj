#!/usr/local/bin/clojure

(ns fc4c.core
  (:require [clj-yaml.core :as yaml]
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
  :fn (fn [{:keys [ret args]}]
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
                  (let [m (into {} (remove (comp blank-nil-or-empty? second) el))]
                    (when (seq m) m))
                  el))
            in))

(s/def ::name string?)
(s/def ::type #{"element" "relationship" "System Landscape" "System Context" "Container" "Person" "Software System"})
(s/def ::scope string?)
(s/def ::position ::coord-string)
(s/def ::description string?)
(s/def ::tags string?)
(s/def ::technology string?)
(s/def ::container (s/keys :req [::name ::type ::position]
                           :opt [::description ::tags ::technology]))
(s/def ::containers (s/coll-of ::container))
(s/def ::element
  (s/keys :req [::name ::type ::position]
          :opt [::description ::containers ::tags]))
(s/def ::elements (s/coll-of ::element))

(s/def ::diagram
  (s/keys :req [::type ::scope ::description ::elements ::relationships ::styles ::size]))

(s/fdef shrink
  :args (s/cat :in ::diagram)
  :ret (s/nilable map?)
  :fn (fn [{{in :in} :args, ret :ret}]
        (let [leaf-vals #(->> (tree-seq map? vals %)
                              (filter (complement map?))
                              flatten)
              in-vals (->> (leaf-vals in) (filter (complement blank-nil-or-empty?)))
              ret-vals (leaf-vals ret)]
          (if (seq in-vals)
              (and (not-any? blank-nil-or-empty? ret-vals)
                   (= in-vals ret-vals))
              (nil? ret)))))

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

(def coord-pattern #"^(\d{1,4}), ?(\d{1,4})$")

(s/def ::coord-string
  (s/with-gen string?
    ;; unfortunately we can’t use coord-pattern here because coord-pattern has anchors
    ;; which are not supported by string-from-regex.
    #(gen'/string-from-regex #"(\d{1,4}), ?(\d{1,4})")))

(s/def ::coord-int
  ;; The upper bound here was semi-randomly chosen; we just need a reasonable number that a real
  ;; diagram is unlikely to ever need but that won’t cause integer overflows when multiplied.
  ;; In other words, we’re using int-in rather than nat-int? because sometimes the generator for
  ;; nat-int? returns very very large integers, and those can sometimes blow up the functions
  ;; during generative testing.
  (s/int-in 0 50000))

(defn parse-coords [s]
  (some->> s
           (re-find coord-pattern)
           (drop 1)
           (map #(Integer/parseInt %))))

(s/fdef parse-coords
  :args (s/cat :s ::coord-string)
  :ret (s/coll-of ::coord-int :count 2)
  :fn (fn [{:keys [ret args]}]
        (= ret
           (->> (split (:s args) #",") 
                (map trim)
                (map #(Integer/parseInt %))))))

(s/def ::snap-target #{10 25 50 75 100})

(defn round-to-closest [target n]
  (case n
    0 0
    (-> (/ n (float target))
        Math/round
        (* target))))

(s/fdef round-to-closest
  :args (s/cat :target ::snap-target
               :n ::coord-int)
  :ret ::coord-int
  :fn (fn [{:keys [ret args]}]
        (let [{:keys [target n]} args
              remainder (case ret 0 0 (rem ret target))]
          (zero? remainder))))

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
  :ret ::coord-string
  :fn (fn [{:keys [ret args]}]
        (let [parsed-ret (parse-coords ret)
              {:keys [:to-closest :min-margin]} args]
         (every? #(>= % min-margin) parsed-ret))))

(defn snap-elem-to-grid
  "Accepts an ordered map representing an element (a software system, person, container, or
  component) and snaps its position (coords) to a grid using the specified values."
  [elem to-closest min-margin]
  (let [coords (parse-coords (::position elem))
        offsets (get elem-offsets (::type elem) (repeat 0))
        new-coords (snap-coords coords to-closest min-margin offsets)]
    (assoc elem ::position new-coords)))

(s/fdef snap-elem-to-grid
  :args (s/cat :elem ::elem
               :to-closest ::snap-target
               :min-margin nat-int?)
  :ret ::elem
  :fn (fn [{{:keys [elem to-closest min-margin]} :args, ret :ret}]
        (= (::position ret)
           (-> (::position elem)
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
