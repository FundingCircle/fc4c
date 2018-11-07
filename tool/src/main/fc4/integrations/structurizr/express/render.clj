(ns fc4.integrations.structurizr.express.render
  (:require [clojure.java.shell :refer [sh]]))

(defn render
  "Renders a Structurizr Express diagram as a PNG file, returning a PNG
  bytearray. Not entirely pure; spawns a child process to perform the rendering.
  FWIW, that process is stateless and ephemeral."
  [diagram-yaml]
  ;; TODO: use ProcessBuilder (or some Clojure wrapper for such) rather than sh
  ;; so we can stream output from stderr to stderr so we can display progress as
  ;; it happens, so the user knows that something is actually happening!
  (let [result (sh "renderer/render.js"
                   :in diagram-yaml
                   :out-enc :bytes)
        {:keys [exit out err]} result]
    (if (zero? exit)
      out
      (throw (Exception. err)))))

(comment
  (use :reload 'fc4.integrations.structurizr.express.render 'clojure.java.io 'clojure.java.shell)

  ; diagram-yaml
  (def dy (slurp "test/data/structurizr/express/diagram_valid_cleaned.yaml"))

  ; png-bytes
  (def pngb (render dy))

  (defn binary-spit [file-path data]
    (with-open [out (output-stream (file file-path))]
      (.write out data)))

  (binary-spit "/tmp/diagram.png" pngb))
