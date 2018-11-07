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
            [mikera.image.filters                               :refer [contrast]])
  (:import  [java.awt Color Toolkit]
            [java.awt.image BufferedImage FilteredImageSource RGBImageFilter]
            [java.io ByteArrayInputStream ByteArrayOutputStream DataInputStream]
            [java.util Arrays]
            [javax.imageio ImageIO]))

(defn file->bytes
  "Copied from https://stackoverflow.com/a/29640320/7012"
  [^java.io.File file]
  (let [result (byte-array (.length file))]
    (with-open [in (DataInputStream. (input-stream file))]
      (.readFully in result))
    result))

(def toolkit (Toolkit/getDefaultToolkit))
(def watermark-color (Color. 0.99 0.99 0.99))
(def white-rgb (.getRGB Color/white))

(def watermark-filter
  (proxy
    [RGBImageFilter]
    []
    (filterRGB [_x _y rgb]
      ; (let [color-vals (-> (Color. rgb) (.getRGBColorComponents nil))]
        ; (if (every? #(> % 0.8) color-vals)
          white-rgb
          ; rgb
          )))
          ;))

(defn image->buffered-image [image]
  (if (instance? BufferedImage image)
    image
    (let [buffered-image (BufferedImage. (.getWidth image nil)
                                         (.getHeight image nil)
                                         BufferedImage/TYPE_INT_ARGB)]
      (doto (.createGraphics buffered-image)
            (.drawImage image 0 0 nil)
            (.dispose))
      buffered-image)))

(defn bytes->buffered-image [bytes]
  (ImageIO/read (ByteArrayInputStream. bytes)))

(defn image->bytes [image]
  (with-open [baos (ByteArrayOutputStream.)]
    (ImageIO/write (image->buffered-image image) "png" baos)
    (.toByteArray baos)))

(defn remove-watermark [img-bytes]
  (let [in-image (ImageIO/read (input-stream img-bytes))
        source (FilteredImageSource. (.getSource in-image) watermark-filter)
        out-image (.createImage toolkit source)]
    (image->bytes out-image)))

(defn binary-spit [f data]
  (with-open [out (io/output-stream (file f))]
    (.write out data)))

(defn adjust-contrast [ratio image-bytes]
  (-> (bytes->buffered-image image-bytes)
      ((contrast ratio))
      (image->bytes)))

(defn temp-png-file [basename] (java.io.File/createTempFile basename ".png"))

(deftest render
  (testing "happy paths"
    (testing "rendering a Structurizr Express file"
      ;; NB: this approach of blowing out the image contrast and then checking
      ;; whether the images are then *exactly* identical, byte-for-byte, is not
      ;; great. For one thing, it removes light lines and fills, and it’d be
      ;; better to actually check those. So:
      ;; TODO: compare image *difference* and consider anything 90+% similar to
      ;; be equivalent.
      (let [blow-out (partial adjust-contrast 5.0)
            dir "test/data/structurizr/express/"
            yaml (slurp (str dir "diagram_valid_cleaned.yaml"))
            {:keys [::r/png-bytes ::r/stderr] :as result} (r/render yaml)

            actual-bytes (blow-out png-bytes)
            expected-file (file (str dir "diagram_valid_cleaned_expected.png"))
            expected-bytes (-> (file->bytes expected-file)
                               (blow-out))]

        (is (s/valid? ::r/result result) (s/explain-str ::r/result result))

        (is (Arrays/equals actual-bytes expected-bytes)
            ;; NB: below in addition to returning a message we write the actual
            ;; bytes out to the file system, to help with debugging. But
            ;; apparently `is` evaluates this `msg` arg eagerly, so it’s
            ;; evaluated even if the assertion is true. This means that even
            ;; when the test passes the “expected” file is written out to the
            ;; filesystem. So TODO: maybe we should do something about this.
            (let [actual-file (temp-png-file "diagram_valid_cleaned_actual.png")]
              (binary-spit actual-file actual-bytes)
              (str stderr
                   "\nfile with “expected” PNG: " expected-file
                   "\nactual PNG written to " (.getPath actual-file))))))))
