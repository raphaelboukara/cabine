(defproject cabine "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2657"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [viebel/audyx-toolbet "0.0.10" :exclusions [org.clojure/clojure]]
                 [viebel/webaudio "0.0.10"]
                 [om "0.8.0-rc1"]]

  :plugins [[lein-cljsbuild "1.0.4"]]

  :source-paths ["src" "target/classes"]

  :clean-targets ["out/cabine" "cabine.js"]

  :cljsbuild {
    :builds [{:id "cabine"
              :source-paths ["src"]
              :compiler {
                :output-to "app.js"
                :output-dir "out"
                :optimizations :none
                :cache-analysis true
                :source-map true}}]})
