(defproject com.brunobonacci/easy-subnet "0.4.1"
  :description "easy subnetting tool"

  :url "https://github.com/BrunoBonacci/easy-subnet"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :scm {:name "git" :url "https://github.com/BrunoBonacci/easy-subnet.git"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [prismatic/schema "1.1.10"]
                 [org.clojure/tools.cli "0.4.2"]
                 [com.brunobonacci/where "0.5.1"]
                 [org.clojure/data.json "0.2.6"]]

  :main com.brunobonacci.easy-subnet

  ;;:global-vars {*warn-on-reflection* true}

  :jvm-opts ["-server"]

  :uberjar-name "easy-subnet-standalone.jar"

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
                                  [lein-shell "0.5.0"]
                                  [lein-binplus "0.6.5"]]}}

  :aliases
  {;; Assumes local machine is a Mac
   "native-mac"
   ["shell"
    "native-image" "--report-unsupported-elements-at-runtime"
    "-jar" "target/easy-subnet-standalone.jar"
    "-H:Name=target/easy-subnet-Darwin-x86_64" ]

   ;; assumes container on Mac with /tmp shared with DockerVM
   "native-linux"
   ["do"
    "shell" "mkdir" "-p" "/tmp/target/,"
    "shell" "cp" "./target/easy-subnet-standalone.jar" "/tmp/target/,"
    "shell"
    "docker" "run" "-v" "/tmp:/easy-subnet" "findepi/graalvm:all"
    "/graalvm/bin/native-image" "--report-unsupported-elements-at-runtime"
    "-jar" "/easy-subnet/target/easy-subnet-standalone.jar"
    "-H:Name=/easy-subnet/target/easy-subnet-Linux-x86_64,"
    "shell" "cp" "/tmp/target/easy-subnet-Linux-x86_64" "./target/"

    ;; docker run -ti -v /tmp:/easy-subnet findepi/graalvm:all /bin/bash
    ;; /graalvm/bin/native-image  --report-unsupported-elements-at-runtime -jar /easy-subnet/target/easy-subnet-standalone.jar -H:Name=/easy-subnet/target/easy-subnet-Linux-x86_64
    ;;
    ]

   "native"
   ["do" "clean," "bin," "native-mac," "native-linux"]

   ;; prep release upload
   "package-native"
   ["do" "shell" "./bin/package-native.sh"]
   }
  )
