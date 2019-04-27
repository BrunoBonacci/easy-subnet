(ns perf
  (:require [com.brunobonacci.easy-subnet :refer :all]
            [criterium.core :refer [bench quick-bench]]))


(comment

  ;; perf evaluation

  (bench
   (ip->num "192.168.23.43"))

  )
