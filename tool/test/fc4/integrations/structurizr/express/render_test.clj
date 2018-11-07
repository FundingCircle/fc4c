; Suppress the Java icon from popping up and grabbing focus on MacOS.
; Found in a comment to this answer: https://stackoverflow.com/a/17544259/7012
; This is at the top of the file, as opposed to the ns form being first as is
; idiomatic, because one of the mikera.image namespaces triggers that Java icon
; on load.
(System/setProperty "apple.awt.UIElement" "true")

(ns fc4.integrations.structurizr.express.render-test
  (:require [fc4.integrations.structurizr.express.render :as r]
            [clojure.java.io                             :as io :refer [file input-stream]]
            [clojure.spec.alpha                          :as s]
            [clojure.test                                       :refer [deftest testing is]]
            [image-resizer.core :refer [resize]])
  (:import  [java.awt Color]
            [java.awt.image BufferedImage]
            [java.io ByteArrayInputStream DataInputStream]
            [javax.imageio ImageIO]))

(defn binary-slurp
  "Based on https://stackoverflow.com/a/29640320/7012"
  [file-or-file-path]
  (let [file (file file-or-file-path) ; no-op if the value is already a File
        result (byte-array (.length file))]
    (with-open [in (DataInputStream. (input-stream file))]
      (.readFully in result))
    result))

(defn binary-spit [f data]
  (with-open [out (io/output-stream (file f))]
    (.write out data)))

(defn bytes->buffered-image [bytes]
  (ImageIO/read (ByteArrayInputStream. bytes)))

(defn temp-png-file [basename] (java.io.File/createTempFile basename ".png"))

(defn pixel-diff
  "Ported from https://rosettacode.org/wiki/Percentage_difference_between_images#Java"
  [a b]
  (let [a-color-components (.getRGBColorComponents (Color. a) nil)
        b-color-components (.getRGBColorComponents (Color. b) nil)]
    (->> (map - a-color-components b-color-components)
         (map #(Math/abs %))
         (reduce +))))

(defn image-pixels
  [^BufferedImage img]
  (-> img (.getRaster) (.getDataBuffer) (.getData)))

(defn round-dec
  "Rounds up, e.g. 0.001 rounded to two places will yield 0.01."
  [places d]
  (-> (bigdec d)
      (.setScale places BigDecimal/ROUND_CEILING)
      (double)))

(defn image-diff
  "Ported from https://rosettacode.org/wiki/Percentage_difference_between_images#Java"
  [^BufferedImage a ^BufferedImage b]
  (let [width   (.getWidth a)
        height  (.getHeight a)
        max-diff (* 3 255 width height)]
    (when (or (not= width (.getWidth b))
              (not= height (.getHeight b)))
      (throw (Exception. "Images must have the same dimensions!")))
    (as-> (map pixel-diff (image-pixels a) (image-pixels b)) it
      (reduce + it)
      (/ (* 100.0 it) max-diff)
      (round-dec 4 it))))

(deftest render
  (testing "happy paths"
    (testing "rendering a Structurizr Express file"
      (let [dir "test/data/structurizr/express/"
            yaml (slurp (str dir "diagram_valid_cleaned.yaml"))
            {:keys [::r/png-bytes ::r/stderr] :as result} (r/render yaml)
            actual-bytes png-bytes
            expected-bytes (binary-slurp (str dir "diagram_valid_cleaned_expected.png"))

            difference (->> [actual-bytes expected-bytes]
                            (map bytes->buffered-image)
                            (map #(resize % 1000 1000))
                            (reduce image-diff))

            ;; This threshold might seem low, but the diffing algorithm is
            ;; giving very low results for some reason. This threshold seems
            ;; to be sufficient to make the random watermark effectively ignored
            ;; while other, more significant changes (to my eye) seem to be
            ;; caught. Still, this is pretty unscientific, so it might be worth
            ;; looking into making this more precise and methodical.
            threshold 0.003]

        (is (s/valid? ::r/result result) (s/explain-str ::r/result result))

        (is (<= difference threshold)
            ;; NB: below in addition to returning a message we write the actual
            ;; bytes out to the file system, to help with debugging. But
            ;; apparently `is` evaluates this `msg` arg eagerly, so it’s
            ;; evaluated even if the assertion is true. This means that even
            ;; when the test passes the “expected” file is written out to the
            ;; filesystem. So TODO: maybe we should do something about this.
            (let [expected-debug-fp (temp-png-file "rendered_expected.png")
                  actual-debug-fp (temp-png-file "rendered_actual.png")]
              (binary-spit expected-debug-fp expected-bytes)
              (binary-spit actual-debug-fp actual-bytes)
              (str stderr
                   "Images are "
                   difference
                   " different, which is higher than the threshold of "
                   threshold
                   "\n“expected” PNG written to:" (.getPath expected-debug-fp)
                   "\n“actual” PNG written to:" (.getPath actual-debug-fp))))))))
