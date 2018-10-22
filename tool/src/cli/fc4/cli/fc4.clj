(ns fc4.cli.fc4
  "Main CLI command that invokes subcommands."
  (:require
   [clojure.string   :as str     :refer [join]]
   [fc4.cli.export   :as export]
   [fc4.cli.render   :as render]
   [fc4.cli.util     :as cu      :refer [exit]]
   [fc4.cli.wcb      :as wcb]))

(def subcommands
  {:export export/-main
   :render render/-main
   :wcb    wcb/-main})

(defn invalid-subcommand-message [subcommand]
  (str subcommand
       " is not a valid subcommand.\nValid subcommands are: "
       (join ", " (map name (keys subcommands)))))

(defn -main
  ;; NB: if and when we add “universal” options — options that apply to all
  ;; subcommands — then we’ll probably want to use tools.cli/parse-opts to parse
  ;; them. It includes functionality for parsing in cases such as these wherein
  ;; a program is “divided” into subcommands. See
  ;;   https://github.com/clojure/tools.cli#in-order-processing-for-subcommands
  ;;
  ;; TODO: Actually, now that I think about it, we should probably add a --help
  ;; universal option ASAP.
  [subcommand & rest-args]
  (if-let [f (get subcommands (keyword subcommand))]
    (do (apply f rest-args)
        ;; I’m not sure why, but without this the render subcommand would delay
        ;; the exit of the main command by about a minute. TODO: debug.
        (System/exit 0))
    (exit 1 (invalid-subcommand-message subcommand))))
