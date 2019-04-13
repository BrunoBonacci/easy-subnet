(defproject com.brunobonacci/easy-subnet "0.1.0"
  :description "easy subnetting tool"

  :url "https://github.com/BrunoBonacci/easy-subnet"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :scm {:name "git" :url "https://github.com/BrunoBonacci/easy-subnet.git"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [prismatic/schema "1.1.10"]
                 [org.clojure/tools.cli "0.4.2"]
                 [kovacnica/clojure.network.ip "0.1.3b"
                  :exclusions [org.clojure/clojurescript]]
                 [com.brunobonacci/where "0.5.1"]]

  :main com.brunobonacci.easy-subnet

  ;;:global-vars {*warn-on-reflection* true}

  :jvm-opts ["-server"]

  :bin {:name "easy-subnet"
        :bin-path "~/bin"
        :jvm-opts ["-server" "-Dfile.encoding=utf-8" "$JVM_OPTS" ]}

  :profiles {:uberjar {:jvm-opts ["-Dclojure.compiler.direct-linking=true"]
                       :aot :all}
             :dev {:dependencies [[midje "1.9.8"]
                                  [org.clojure/test.check "0.10.0-alpha4"]
                                  [criterium "0.4.4"]
                                  [org.slf4j/slf4j-log4j12 "1.8.0-beta4"]]
                   :resource-paths ["dev-resources"]
                   :plugins      [[lein-midje "3.2.1"]
                                  [lein-binplus "0.6.5"]]}}
  )
