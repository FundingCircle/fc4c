(ns fc4.cli.render
  "CLI subcommand that renders Structurizr Express diagrams into PNG image
  files."
  (:require
   [clojure.java.io   :as io                      :refer [file output-stream]]
   [clojure.string    :as string]
   [fc4.integrations.structurizr.express.render   :refer [render]]))

(defn -main
  ;; NB: if and when we add options we’ll probably want to use
  ;; tools.cli/parse-opts to parse them.
  ;;
;; TODO: Actually, now that I think about it, we should probably add a --help
;; option ASAP.
  [& in-paths]
  (doseq [in-path in-paths]
    (let [yaml      (slurp in-path)
          png-bytes (render yaml)
          out-path  (string/replace in-path #"\.ya?ml$" ".png")]
      (with-open [out (output-stream (file out-path))]
        (.write out png-bytes)))))
