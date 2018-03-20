(ns fc4c.spec
  (:require [clojure.spec.alpha :as s]
            [com.gfredericks.test.chuck.generators :as gen']))

(s/def ::name string?)
(s/def ::description string?)
(s/def ::tags string?)
(def coord-pattern-base "(\\d{1,4}), ?(\\d{1,4})")
(s/def ::coord-string
  (s/with-gen string?
    ;; unfortunately we can’t use coord-pattern here because it has anchors
    ;; which are not supported by string-from-regex.
    #(gen'/string-from-regex (re-pattern coord-pattern-base))))
(s/def ::coord-int
  ;; The upper bound here was semi-randomly chosen; we just need a reasonable number that a real
  ;; diagram is unlikely to ever need but that won’t cause integer overflows when multiplied.
  ;; In other words, we’re using int-in rather than nat-int? because sometimes the generator for
  ;; nat-int? returns very very large integers, and those can sometimes blow up the functions
  ;; during generative testing.
  (s/int-in 0 50000))


(ns fc4c.spec.element
  (:require [clojure.spec.alpha :as s]
            [fc4c.spec :as fs]
            [com.gfredericks.test.chuck.generators :as gen']))

(s/def ::type #{"Container" "Person" "Software System"})
(s/def ::position ::fs/coord-string)
(s/def ::technology string?)
(s/def ::container (s/keys :req [::fs/name ::type ::position]
                           :opt [::fs/description ::fs/tags ::technology]))
(s/def ::containers (s/coll-of ::container))
(s/def ::element
  (s/keys :req [::fs/name ::type ::position]
          :opt [::fs/description ::containers ::fs/tags]))




(ns fc4c.spec.relationship
  (:require [clojure.spec.alpha :as s]
            [fc4c.spec :as fs]
            [com.gfredericks.test.chuck.generators :as gen']))

(s/def ::source string?)
(s/def ::destination string?)
(s/def ::vertices (s/coll-of any?)) ;;; TODO: make this more specific!
(s/def ::relationship
  (s/keys :req [::source ::destination]
          :opt [::fs/description ::fs/tags ::vertices]))




(ns fc4c.spec.style
  (:require [clojure.spec.alpha :as s]
            [fc4c.spec :as fs]
            [com.gfredericks.test.chuck.generators :as gen']))

(s/def ::type #{"element" "relationship"})
(s/def ::tag string?)
(s/def ::width pos-int?)
(s/def ::height pos-int?)
(s/def ::color string?) ;;; TODO: Make this more specific
(s/def ::shape #{"Box" "RoundedBox" "Circle" "Ellipse" "Hexagon" "Person" "Folder" "Cylinder" "Pipe"})
(s/def ::background string?) ;;; TODO: Make this more specific
(s/def ::dashed #{"true" "false"})
(s/def ::border #{"Dashed" "Solid"})
(s/def ::style
  (s/keys :req [::type ::tag]
          :opt [::color ::shape ::background ::dashed ::border ::width ::height]))




(ns fc4c.spec.diagram
  (:require [clojure.spec.alpha :as s]
            [fc4c.spec :as fs]
            [fc4c.spec.element :as fse]
            [fc4c.spec.relationship :as fsr]
            [fc4c.spec.style :as fss]
            [com.gfredericks.test.chuck.generators :as gen']))

(s/def ::type #{"System Landscape" "System Context" "Container"})
(s/def ::scope string?)
(s/def ::size string?) ;;; TODO: Make this more specific
(s/def ::elements (s/coll-of ::fse/element))
(s/def ::relationships (s/coll-of ::fsr/relationship))
(s/def ::styles (s/coll-of ::fss/style))
(s/def ::diagram
  (s/keys :req [::type ::scope ::fs/description ::elements ::relationships ::styles ::size]))
