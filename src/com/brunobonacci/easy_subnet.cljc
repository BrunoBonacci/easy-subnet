(ns com.brunobonacci.easy-subnet
  (:require [clojure.network.ip :as net]
            [clojure.string :as str]
            [where.core :refer [where]]
            [clojure.tools.cli :refer [parse-opts]]
            [schema.core :as s])
  (:gen-class))



(def spec-schema
  (s/conditional map? {s/Str (s/recursive #'spec-schema)} :else [String]))



(defn network [cidr]
  (let [inet (net/make-network (str cidr))
        network (str (first inet) "/" (net/network-mask inet))]
    {:network network
     :first   (str (first inet))
     :bcast   (str (last inet))
     :size    (count inet)
     :bitmask (net/network-mask inet)
     :ip-num  (net/numeric-value inet)}))



(defn pad-power-2 [coll]
  (when (= 0 (count coll))
    (throw (ex-info "The collection must have at least 1 element." {:collection coll})))
  (let [bits (int (Math/ceil (/ (Math/log (count coll)) (Math/log 2))))
        pad  (map (fn [n] (str "free/" (inc n))) (range 10))
        size (Math/pow 2 bits)]
    (->> (partition size size pad coll) first vec)))



(defn fill-free-slots
  [spec]
  (cond
    (sequential? spec) (pad-power-2 spec)
    (map? spec) (let [ks (pad-power-2 (keys spec))]
                  (->> spec
                       (map (fn [[k v]] [k (fill-free-slots v)]))
                       (into {})
                       (#(reduce (fn [m k]
                                   (if (get m k)
                                     m
                                     (assoc m k [k]))) % ks))))))



(defn divider [cidr spec]
  (s/validate spec-schema spec)
  (let [spec (fill-free-slots spec)]
    (->>
     (net/divide-network (net/make-network cidr) (count spec))
     (map (fn [id n] [id (network n)]) spec)
     (map (fn [[id n]]
            (if (map-entry? id)
              [(first id) (divider (:network n) (second id))]
              [id n])))
     (into {}))))



(defn unnest
  ([m]
   (unnest map? m))
  ([pred? m]
   (->> m
        (unnest pred? [])
        (partition 2)
        (map vec)
        (into {})))
  ([pred? p m]
   (->> m
        (mapcat (fn [[k v]]
                  (if (pred? v)
                    (unnest pred? (conj p k)  v)
                    [(conj p k) v]))))))



;; adjusted from clojure.pprint
;; https://github.com/clojure/clojure/blob/master/src/clj/clojure/pprint/print_table.clj
(defn print-table
  "Prints a collection of maps in a textual table. Prints table headings
   ks, and then a line of output for each row, corresponding to the keys
   in ks. If ks are not specified, use the keys of the first item in rows."
  {:added "1.3"}
  ([ks rows]
   (when (seq rows)
     (let [widths (map
                   (fn [k]
                     (apply max (count (str k)) (map #(count (str (get % k))) rows)))
                   ks)
           spacers (map #(apply str (repeat % "-")) widths)
           fmts (map #(str "%-" % "s") widths)
           fmt-row (fn [leader divider trailer row]
                     (str leader
                          (apply str
                                 (interpose
                                  divider
                                  (for [[col fmt] (map vector (map #(get row %) ks) fmts)]
                                    (format fmt (str col)))))
                          trailer))]
       (println)
       (println (fmt-row "| " " | " " |" (zipmap ks ks)))
       (println (fmt-row "|-" "-+-" "-|" (zipmap ks spacers)))
       (doseq [row rows]
         (println (fmt-row "| " " | " " |" row))))))
  ([rows] (print-table (keys (first rows)) rows)))



(defn display-table
  ([networds]
   (display-table {} networds))
  ([{:keys [order-by select]
     :or {order-by :name select (constantly true)}} networks]
   (->> networks
        (unnest (fn [m] (and (map? m) (not (:network m)))))
        (map (fn [[k v]] (assoc v :name (str/join "." (map name k)))))
        (filter select)
        (sort-by order-by)
        (print-table [:name :network :first :bcast :size]))))



(def cli-options

  [["-c" "--cidr CIDR" "CIDR of the subnet to split"
    :validate [(partial re-matches #"\d+\.\d+\.\d+\.\d+/\d+")
               "Must be a valid CIDR like 10.10.0.0/16"]]

   ["-l" "--layout LAYOUT" "The layout of how to split the subnets"
    :parse-fn read-string
    :validate [identity
               "Must provide a layout to split. see documentation. "]]

   ["-p" "--print SELECTION" "Displays a table with the given selection.
                                Can be one of: `both`, `free`, `nets`, default `both`"
    :parse-fn keyword
    :default :both
    :validate [#{:both :nets :free} "Mus be one of: `both`, `free`, `nets`"]]

   ["-o" "--order ORDER" "Diplay ordering: name, net, (default: net)"
    :parse-fn keyword
    :default :net
    :validate [#{:name :net} "Mus be one of: `name`, `net`"]]

   [nil  "--stacktrace" "Display full stacktrace in case of errors"]
   ["-h" "--help"]])



(defn usage [options-summary]
  (->> [""
        "     --=  Easy Subnetting Tool =--"
        "  (v0.1.0) - (C) Bruno Bonacci - 2019"
        ""
        "Usage: easy-subnet -c 10.10.0.0/16 -l '{\"dc1\" [\"net1\" \"net2\"], \"dc2\" [\"net1\" \"net2\" \"net3\"]}'"
        ""
        "Options:"
        options-summary
        ""
        "Please refer to the following page for more information:"
        "https://github.com/BrunoBonacci/easy-subnet"
        ""]
       (str/join \newline)))



(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))



(defn validate-args
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      (not (and (:cidr options) (:layout options)))
      {:exit-message (usage summary)}

      :else
      {:action :subnet
       :options options
       :select (case (:print options)
                 :both (constantly true)
                 :nets (where :name :not-contains? "free")
                 :free (where :name :contains? "free"))
       :order (case (:order options)
                :name :name
                :net  :ip-num)})))



(defn exit [status msg]
  (println msg)
  (System/exit status))



(defmacro show-stacktrace!!
  [show & body]
  {:style/indent 1}
  `(try ~@body
        (catch Throwable x#
          (if ~show
            (.printStackTrace x#)
            (do
              (.println System/err (str "ERROR: " (.getMessage x#)))
              (.println System/err (str "CAUSE: " (loop [e# x#]
                                                    (if (.getCause e#)
                                                      (recur (.getCause e#))
                                                      (.getMessage e#)))))))
          (System/exit 1))))



(defn -main [& args]
  (let [{:keys [order select options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (show-stacktrace!! (:stacktrace options)
        (->> (:layout options)
             (divider (:cidr options))
             (display-table {:select select :order-by order}))))))
