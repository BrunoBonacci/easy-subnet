(ns user
  (:require [com.brunobonacci.subnet :refer :all]))


(comment

  (def spec
    ["net1" "net2"])


  (def spec
    {"dc1" ["net1" "net2"]
     "dc2" ["net1" "net2"]})


  (def spec
    {"small" {"mgmt"   ["az1" "az2" "az3"]
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


  (def cidr "10.15.0.0/16")

  (->> spec
       (divider cidr)
       (display-table {:select (where :name :not-contains? "free")
                       :order-by :ip-num}))


  (->> spec
       (divider cidr)
       (display-table {:select (where :name :contains? "free")
                       :order-by :ip-num}))

  (fill-free-slots spec)
  )
