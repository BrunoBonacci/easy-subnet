(ns com.brunobonacci.easy-subnet
  (:require [clojure.string :as str]
            [where.core :refer [where]]
            [clojure.tools.cli :refer [parse-opts]]
            [schema.core :as s]
            [clojure.java.io :as io]
            [clojure.pprint :refer [cl-format]]
            [clojure.data.json :as json])
  (:gen-class))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                  ----==| C O R E   I P   C A L C |==----                   ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn binary-ip4 [ip-num]
  (cl-format nil "~32,'0',B" ip-num))


(def ^:const powers-of2
  (vec (take 129 (iterate (fn [v] (*' 2 v)) 1))))


(defn ip->num [ip]
  (->> ip
     (re-find #"^(\d+)\.(\d+)\.(\d+)\.(\d+)$")
     rest
     (mapv #(Integer/parseInt %))
     ((fn [[a b c d]]
        (+ (* a (powers-of2 24))
           (* b (powers-of2 16))
           (* c (powers-of2 8))
           d)))))


(defn bit-mask
  [size]
  (dec (powers-of2 size)))


(defn cidr-bit-mask
  [max size]
  (- (dec (powers-of2 max))
     (dec (powers-of2 (- max size)))))


(defn big-and [a b]
  (.and
   (.toBigInteger (bigint a))
   (.toBigInteger (bigint b))))


(defn big-or [a b]
  (.or
   (.toBigInteger (bigint a))
   (.toBigInteger (bigint b))))


(defn big-not [a]
  (.not
   (.toBigInteger (bigint a))))


(defn big-shift-right [n a]
  (.shiftRight
   (.toBigInteger (bigint a))
   n))


(defn big-shift-left [n a]
  (.shiftLeft
   (.toBigInteger (bigint a))
   n))


(defn ip-num->parts
  [ip-num]
  [(big-shift-right 24 (big-and ip-num (big-and (bit-mask 32) (big-not (bit-mask 24)))))
   (big-shift-right 16 (big-and ip-num (big-and (bit-mask 24) (big-not (bit-mask 16)))))
   (big-shift-right 8  (big-and ip-num (big-and (bit-mask 16) (big-not (bit-mask 8)))))
   (big-and ip-num (bit-mask 8))])


(defn ip-num->ip
  [ip-num]
  (->> ip-num ip-num->parts (str/join ".")))


(defn network
  ([cidr]                                                        ;; TODO: FIX
   (let [[ip mask] (rest (re-find #"^(\d+\.\d+\.\d+\.\d+)/(\d+)$" (str cidr)))]
     (network ip (Integer/parseInt mask))))
  ([ip cidr-mask-size]
   (let [ip-num    (ip->num ip)
         net-num   (big-and ip-num (cidr-bit-mask 32 cidr-mask-size))
         bcast-num (big-or net-num (bit-mask (- 32 cidr-mask-size)))]
     {:network   (str (ip-num->ip net-num) "/" cidr-mask-size)
      :net-num   net-num
      :ip-num    ip-num
      :first     (ip-num->ip net-num)
      :bitmask   cidr-mask-size
      :type      :ip4
      :bcast     (ip-num->ip bcast-num)
      :net-mask  (ip-num->ip (cidr-bit-mask 32 cidr-mask-size))
      :size      (long (inc (- bcast-num net-num)))})))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                       ----==| D I V I D E R |==----                        ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def spec-schema
  (s/conditional map? {s/Str (s/recursive #'spec-schema)} :else [String]))


(defn next-power-2 [n]
  (long (Math/ceil (/ (Math/log n) (Math/log 2)))))


(defn pad-power-2 [coll]
  (when (= 0 (count coll))
    (throw (ex-info "The collection must have at least 1 element." {:collection coll})))
  (let [bits (next-power-2 (count coll))
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




(defn divide-network
  [{:keys [net-num bitmask size bcast]} parts]
  {:pre [(> parts 0)]}
  (let [bits     (next-power-2 parts)
        _        (assert (>= 32 (+ bitmask bits))
                         "Can't split is so many parts.")
        parts'   (long (Math/pow 2 bits))
        bitmask' (+ bitmask bits)
        size'    (/ size parts')]
    (for [net-num (range net-num (inc (ip->num bcast)) size')]
      (network (ip-num->ip net-num) bitmask'))))



(defn divider
  [cidr spec]
  (s/validate spec-schema spec)
  (let [spec (fill-free-slots spec)]
    (->>
     (divide-network (network cidr) (count spec))
     (map (fn [id n] [id n]) spec)
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




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                       ----==| D I S P L A Y |==----                        ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


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


(defmulti display (fn [opts n] (:format opts)))


(defmethod display :table
  [{:keys [order-by select]
    :or {order-by :name select (constantly true)}} networks]
  (->> networks
     (unnest (fn [m] (and (map? m) (not (:network m)))))
     (map (fn [[k v]] (assoc v :name (str/join "." (map name k)))))
     (filter select)
     (sort-by order-by)
     (print-table [:name :network :first :bcast :size])))



(defmethod display :json
  [opts networks]
  (json/pprint networks))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                  ----==| C O M M A N D   L I N E |==----                   ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def cli-options

  [["-c" "--cidr CIDR" "CIDR of the subnet to split"
    :validate [(partial re-matches #"\d+\.\d+\.\d+\.\d+/\d+")
               "Must be a valid CIDR like 10.10.0.0/16"]]

   ["-l" "--layout LAYOUT" "The layout of how to split the subnets"
    :parse-fn read-string]


   ["-f" "--file-layout LAYOUT" "The layout of how to split the subnets"
    :parse-fn (comp read-string slurp)]


   ["-p" "--print SELECTION" "Displays a table with the given selection.
                                   Can be one of: `both`, `free`, `nets`, default `both`"
    :parse-fn keyword
    :default :both
    :validate [#{:both :nets :free} "Must be one of: `both`, `free`, `nets`"]]

   ["-o" "--order ORDER" "Diplay ordering: name, net, (default: net)"
    :parse-fn keyword
    :default :net
    :validate [#{:name :net} "Must be one of: `name`, `net`"]]

   [nil "--format FORMAT" "The format to display the output. One of: `table`, `json`
                                    (default: `table`)"
    :parse-fn keyword
    :default :table
    :validate [#{:table :json} "Must be one of: `table`, `json`"]]

   [nil "--from IP" "Starting IP for listing"
    :validate [(partial re-matches #"(\d+)\.(\d+)\.(\d+)\.(\d+)") "Must be a valid IP4, like: 10.23.245.12"]]

   [nil "--to IP" "Last IP for listing"
    :validate [(partial re-matches #"(\d+)\.(\d+)\.(\d+)\.(\d+)") "Must be a valid IP4, like: 10.23.245.12"]]

   [nil  "--stacktrace" "Display full stacktrace in case of errors"]
   ["-h" "--help"]])



(defn usage [options-summary]
  (->> [""
      "     --=  Easy Subnetting Tool =--"
      "  (v0.4.1) - (C) Bruno Bonacci - 2019"
      ""
      " - To subnet a given network:"
      "   easy-subnet -c 10.10.0.0/16 -l '{\"dc1\" [\"net1\" \"net2\"], \"dc2\" [\"net1\" \"net2\" \"net3\"]}'"
      ""
      " - To list all the IPs of a subnet:"
      "   easy-subnet list -c 10.10.0.0/16"
      "   easy-subnet list --from 192.168.12.1 --to 192.168.15.1"
      ""
      " - Show network details:"
      "   easy-subnet net -c 10.10.0.0/16"
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
      (:help options)              ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}

      errors                ; errors => exit with description of errors
      {:exit-message (error-msg errors)}

      :else
      (cond
        ;; subentting
        (or (= nil (first arguments)) (= "subnet" (first arguments)))
        (cond
          (and (not (:cidr options)) (not (or (:file-layout options) (:layout options))))
          {:exit-message (usage summary) :ok? true}

          (not (:cidr options))
          {:exit-message (error-msg ["CIDR not provided. Use -c option."])}

          (not (or (:file-layout options) (:layout options)))
          {:exit-message (error-msg ["Layout not provided. Use -l or -f options."])}

          :else
          {:action :subnet
           :options (assoc options :layout (or (:layout options) (:file-layout options)))
           :select (case (:print options)
                     :both (constantly true)
                     :nets (where :name :not-contains? "free")
                     :free (where :name :contains? "free"))
           :order (case (:order options)
                    :name :name
                    :net  :ip-num)})

        ;; listing
        (= "list" (first arguments))
        (cond
          (and (not (:cidr options)) (not (:from options)) (not (:to options)))
          {:exit-message (error-msg ["CIDR not provided. Use -c option (or --from/--to)."])}

          (and (:from options) (not (:to options)))
          {:exit-message (error-msg ["Missing --to IP."])}

          (and (not (:from options)) (:to options))
          {:exit-message (error-msg ["Missing --from IP."])}

          :else
          {:action :list
           :from   (if (:cidr options)
                     (:first (network (:cidr options)))
                     (:from options))
           :to     (if (:cidr options)
                     (:bcast (network (:cidr options)))
                     (:to options))})

        ;; net details
        (= "net" (first arguments))
        (cond
          (not (:cidr options))
          {:exit-message (error-msg ["CIDR not provided. Use -c option."])}

          :else
          {:action :show-net
           :cidr (:cidr options)})


        :else
        {:exit-message (usage summary) :ok? true}))))



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



(defmulti run-action :action)


(defmethod run-action :subnet
  [{:keys [order select options]}]
  (->> (:layout options)
     (divider (:cidr options))
     (display {:select select :order-by order :format (:format options)})))


(defmethod run-action :list
  [{:keys [from to]}]
  (let [start (ip->num from)
        stop  (ip->num to)
        dir   (if (> start stop) -1 1)]
    (->> (range start (+ stop dir) dir)
       (map ip-num->ip)
       (run! println))))


(defmethod run-action :show-net
  [{:keys [cidr]}]
  (let [net   (network cidr)]
    (->> [["network"    (:network net)]
        ["type"         (:type net)]
        ["first-ip"     (:first net)]
        ["broadcast-ip" (:bcast net)]
        ["size"         (:size net)]
        ["network-mask" (:net-mask net)]
        ["bit-mask"     (:bitmask net)]
        ["ip"           (ip-num->ip (:ip-num net))]
        ["bits:ip"      (binary-ip4 (:ip-num net))]
        ["bits:bitmask" (binary-ip4 (cidr-bit-mask 32 (:bitmask net)))]]
       (map (fn [[k v]] {:property k :value v}))
       (print-table [:property :value]))))


(defn -main [& args]
  (let [{:keys [exit-message ok? options] :as cmd} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (show-stacktrace!! (:stacktrace options)
        (run-action cmd)))))


(comment
  (validate-args ["list" "--from" "10.10.2.3" "--to" "10.10.2.7"])

  (validate-args ["-c" "192.168.12.172/28" "-l" (pr-str ["n1" "n2"]) "--stacktrace"])

  )
