(ns fc4.cli.render
  "CLI subcommand that renders Structurizr Express diagrams into PNG image
  files."
  (:require
   [clojure.java.io                             :as io :refer [file output-stream]]
   [clojure.string                              :as string]
   [fc4.integrations.structurizr.express.render :as r]))

(defn println-stderr [& vs]
  (binding [*out* *err*]
    (apply println vs)))

(defn render [yaml]
  (println-stderr "invoking renderer...")
  (let [{stderr ::r/stderr :as result} (r/render yaml)]
    (println-stderr stderr)
    result))

(defn -main
  ;; NB: if and when we add options we’ll probably want to use
  ;; tools.cli/parse-opts to parse them.
  ;;
  ;; TODO: Actually, now that I think about it, we should probably add a --help
  ;; option ASAP.
  ;;
  ;; TODO: Error handling!!!!!
  ;;
  ;; TODO: Check that png-bytes is not empty and exit with an error code if it
  ;; is.
  [& in-paths]
  (doseq [in-path in-paths]
    (let [yaml      (slurp in-path)
          {:keys [::r/png-bytes ::r/stderr]}
          (render yaml)
          out-path  (string/replace in-path #"\.ya?ml$" ".png")]
      (with-open [out (output-stream (file out-path))]
        (.write out png-bytes)))))
