(ns user
  (:require [com.brunobonacci.easy-subnet :refer :all]))


(comment

  (def spec
    ["net1" "net2"])


  (def spec
    {"dc1" ["net1" "net2"]
     "dc2" ["net1" "net2"]})


  (def spec
    {"small" {"mgmt" {"mgmt"   ["az1" "az2" "az3"]
                      "infra"  ["az1" "az2" "az3"]}
              "pub-lb" ["az1" "az2" "az3"]
              "ilb"    ["az1" "az2" "az3"]
              "db"     ["az1" "az2" "az3"]}
     "app"    ["az1" "az2" "az3"]
     "emr"    ["az1" "az2" "az3"]
     "lambda" ["az1" "az2" "az3"]})


  (def spec
    {"small" (for [n  ["mgmt" "pub_lb" "ilb" "db"]
                   az ["az1" "az2" "az3"]]
               (str n "." az))
     "app"    ["az1" "az2" "az3"]
     "emr"    ["az1" "az2" "az3"]
     "lambda" ["az1" "az2" "az3"]})


  (def spec
    {"az1" {"small" ["mgmt" "pub_lb" "ilb" "db"]
            "app"   ["app"]
            "emr"   ["emr"]
            "lambda"   ["lambda"]}
     "az2" {"small" ["mgmt" "pub_lb" "ilb" "db"]
            "app"   ["app"]
            "emr"   ["emr"]
            "lambda"   ["lambda"]}
     "az3" {"small" ["mgmt" "pub_lb" "ilb" "db"]
            "app"   ["app"]
            "emr"   ["emr"]
            "lambda"   ["lambda"]}})



  (def spec
    {"dev"
     {"small" {"mgmt"   ["az1" "az2" "az3" ]
               "pub-lb" ["az1" "az2" "az3" ]
               "ilb"    ["az1" "az2" "az3" ]
               "db"     ["az1" "az2" "az3" ]}
      "app"    ["az1" "az2" "az3" ]
      "emr"    ["az1" "az2" "az3" ]
      "lambda" ["az1" "az2" "az3" ]}
     "uat"
     {"small" {"mgmt"   ["az1" "az2" "az3" ]
               "pub-lb" ["az1" "az2" "az3" ]
               "ilb"    ["az1" "az2" "az3" ]
               "db"     ["az1" "az2" "az3" ]}
      "app"    ["az1" "az2" "az3" ]
      "emr"    ["az1" "az2" "az3" ]
      "lambda" ["az1" "az2" "az3" ]}
     "prd"
     {"small" {"mgmt"   ["az1" "az2" "az3" ]
               "pub-lb" ["az1" "az2" "az3" ]
               "ilb"    ["az1" "az2" "az3" ]
               "db"     ["az1" "az2" "az3" ]}
      "app"    ["az1" "az2" "az3" ]
      "emr"    ["az1" "az2" "az3" ]
      "lambda" ["az1" "az2" "az3" ]}})


  (def spec
    {"mgmt" {"mgmt"   ["az1" "az2" "az3" ]}

     "dev"
     {"public" ["az1" "az2" "az3" ]
      "int-lb" ["az1" "az2" "az3" ]
      "db"     ["az1" "az2" "az3" ]
      "app"    ["az1" "az2" "az3" ]
      "emr"    ["az1" "az2" "az3" ]
      "lambda" ["az1" "az2" "az3" ]}

     "uat"
     {"public" ["az1" "az2" "az3" ]
      "int-lb" ["az1" "az2" "az3" ]
      "db"     ["az1" "az2" "az3" ]
      "app"    ["az1" "az2" "az3" ]
      "emr"    ["az1" "az2" "az3" ]
      "lambda" ["az1" "az2" "az3" ]}

     "prd"
     {"public" ["az1" "az2" "az3" ]
      "int-lb" ["az1" "az2" "az3" ]
      "db"     ["az1" "az2" "az3" ]
      "app"    ["az1" "az2" "az3" ]
      "emr"    ["az1" "az2" "az3" ]
      "lambda" ["az1" "az2" "az3" ]}})


  (def cidr "10.8.0.0/16")

  (->> spec
       (divider cidr)
       (display-table {:select (where :name :not-contains? "free")
                       :order-by :ip-num}))


  (->> spec
       (divider cidr)
       (display-table {:select (where :name :contains? "free")
                       :order-by :ip-num}))

  (fill-free-slots spec)


  (def spec
    {"small"
     ;; management network and LB nets are usually smaller
     {"mgmt" {;; pub mgmt network
              "mgmt"   ["az1" "az2" "az3"]
              ;; private mgmt network for infra tools
              "infra"  ["az1" "az2" "az3"]
              }
      ;; public IPs usually dedicated to LBs
      "pub-lb" ["az1" "az2" "az3"]
      ;; Internal LBs
      "ilb"    ["az1" "az2" "az3"]
      ;; Database networks
      "db"     ["az1" "az2" "az3"]}

     ;; Application private networks
     "app"    ["az1" "az2" "az3"]
     ;; EMR private networks
     "emr"    ["az1" "az2" "az3"]
     ;; lambda private networks
     "lambda" ["az1" "az2" "az3"]})


  (def cidr "10.10.0.0/16")


  (def mapping
    {["small" "mgmt" "mgmt"]  "mgmt_subnets"
     ["small" "mgmt" "infra"] "infra_subnets"
     ["small" "pub-lb"]       "pub_lb_subnets"
     ["small" "ilb" ]         "prv_lb_subnets"
     ["app"]                  "app_subnets"
     ["emr"]                  "emr_subnets"
     ["lambda"]               "lambda_subnets"
     ["small" "db" ]          "database_subnets"

     })

  (def nets
    (->> spec
       (divider cidr)))

  ;; terraform mapping
  (for [[k v] mapping]
    (let [{:strs [az1 az2 az3]} (get-in nets k)]
      ;;[v [(:network az1) (:network az2) (:network az3)]]
      (printf "%-16s = [ %-18s, %-18s, %-18s ]\n" v
              (pr-str (:network az1)),
              (pr-str (:network az2))
              (pr-str (:network az3)))
      ))

  )
