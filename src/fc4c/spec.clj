(ns fc4c.spec
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str :refer [blank?]]
            [com.gfredericks.test.chuck.generators :as gen']))

(s/def :fc4c/non-blank-string (s/and string? (complement blank?)))
(s/def :fc4c/name :fc4c/non-blank-string)
(s/def :fc4c/description :fc4c/non-blank-string)
(s/def :fc4c/tags string?)

(def coord-pattern-base "(\\d{1,4}), ?(\\d{1,4})")

(s/def :fc4c/coord-string
  (s/with-gen string?
    ;; unfortunately we can’t use coord-pattern here because it has anchors
    ;; which are not supported by string-from-regex.
    #(gen'/string-from-regex (re-pattern coord-pattern-base))))
(s/def :fc4c/coord-int
  ;; The upper bound here was semi-randomly chosen; we just need a reasonable number that a real
  ;; diagram is unlikely to ever need but that won’t cause integer overflows when multiplied.
  ;; In other words, we’re using int-in rather than nat-int? because sometimes the generator for
  ;; nat-int? returns very very large integers, and those can sometimes blow up the functions
  ;; during generative testing.
  (s/int-in 0 50000))
(s/def :fc4c/position :fc4c/coord-string)

(s/def :fc4c.container/name :fc4c/name)
(s/def :fc4c.container/type #{"Container"})
(s/def :fc4c.container/position :fc4c/position)
(s/def :fc4c.container/description :fc4c/description)
(s/def :fc4c.container/tags :fc4c/tags)
(s/def :fc4c.container/technology string?)

(s/def :fc4c/container
  (s/keys :req [:fc4c.container/name :fc4c.container/type :fc4c.container/position]
          :opt [:fc4c.container/description :fc4c.container/tags :fc4c.container/technology]))

(s/def :fc4c.element/name :fc4c/name)
(s/def :fc4c.element/type #{"Person" "Software System"})
(s/def :fc4c.element/position :fc4c/position)
(s/def :fc4c.element/description :fc4c/description)
(s/def :fc4c.element/containers (s/coll-of :fc4c/container))
(s/def :fc4c.element/tags :fc4c/tags)

(s/def :fc4c/element
  (s/keys :req [:fc4c.element/name :fc4c.element/type :fc4c.element/position]
          :opt [:fc4c.element/description :fc4c.element/containers :fc4c.element/tags]))

(s/def :fc4c.relationship/source :fc4c/name)
(s/def :fc4c.relationship/destination :fc4c/name)
(s/def :fc4c.relationship/description :fc4c/description)
(s/def :fc4c.relationship/tags :fc4c/tags)
(s/def :fc4c.relationship/vertices (s/coll-of :fc4c/coord-string))

(s/def :fc4c/relationship
  (s/keys :req [:fc4c.relationship/source :fc4c.relationship/destination]
          :opt [:fc4c/description :fc4c/tags :fc4c.relationship/vertices]))

(s/def :fc4c.style/type #{"element" "relationship"})
(s/def :fc4c.style/tag :fc4c/non-blank-string)
(s/def :fc4c.style/width :fc4c/coord-int)
(s/def :fc4c.style/height :fc4c/coord-int)
(s/def :fc4c.style/color :fc4c/non-blank-string) ;;; TODO: Make this more specific
(s/def :fc4c.style/shape #{"Box" "RoundedBox" "Circle" "Ellipse" "Hexagon"
                           "Person" "Folder" "Cylinder" "Pipe"})
(s/def :fc4c.style/background :fc4c/non-blank-string) ;;; TODO: Make this more specific
(s/def :fc4c.style/dashed #{"true" "false"})
(s/def :fc4c.style/border #{"Dashed" "Solid"})

(s/def :fc4c/style
  (s/keys :req [:fc4c.style/type :fc4c.style/tag]
          :opt [:fc4c.style/color :fc4c.style/shape :fc4c.style/background
                :fc4c.style/dashed :fc4c.style/border :fc4c.style/width
                :fc4c.style/height]))

(s/def :fc4c.diagram/type #{"System Landscape" "System Context" "Container"})
(s/def :fc4c.diagram/scope :fc4c/non-blank-string)
(s/def :fc4c.diagram/description :fc4c/description)
(s/def :fc4c.diagram/size #{"A2_Landscape" "A3_Landscape"}) ;;; TODO: Add the rest of the options
(s/def :fc4c.diagram/elements (s/coll-of :fc4c/element :min-count 1))
(s/def :fc4c.diagram/relationships (s/coll-of :fc4c/relationship :min-count 1))
(s/def :fc4c.diagram/styles (s/coll-of :fc4c/style :min-count 1))

(s/def :fc4c/diagram
  (s/keys :req [:fc4c.diagram/type :fc4c.diagram/scope :fc4c.diagram/description
                :fc4c.diagram/elements :fc4c.diagram/relationships :fc4c.diagram/styles
                :fc4c.diagram/size]))
