(defproject spam "1.0.0"
  :description "A simple spam filter based on the one in Practical Common Lisp"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [incanter/incanter-core "1.3.0"]]
  :dev-dependencies [[clojure-source "1.3.0"]]
  :jvm-opts ["-Xmx1g"]
  :main spam.core
  :jar-name "spam-filter.jar"
  :uberjar-name "spam-filter-standalone.jar")