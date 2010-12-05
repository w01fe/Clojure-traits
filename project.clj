(defproject angelic "1.0.0-SNAPSHOT"
  :description "Angelic hierarchical planning"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
;                [org.clojars.robertpfeiffer/vijual "0.1.0-SNAPSHOT"]
                 [org.swinglabs/pdf-renderer "1.0.5"]
                 [incanter "1.2.3-SNAPSHOT"]
                 ]
  :dev-dependencies [[swank-clojure "1.2.1"] [mycroft/mycroft "0.0.2"]]
  :jvm-opts ["-server" "-Xmx1g"
             "-agentpath:/Applications/YourKit_Java_Profiler_9.0.0.app/bin/mac/libyjpagent.jnilib"
             #_ "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5000" ]
  )

(ns leiningen.jswat
  (:use
   [clojure.contrib.shell-out :only [sh]]
   [leiningen.classpath]
   [clojure.string :only [join]]))

(defn jswat
  ([project] (jswat project "/Users/jawolfe/Library/clojure/jswat-4.5/bin/jswat"))
  ([project & [path]]
     (let [cp (join java.io.File/pathSeparatorChar (get-classpath project))]
       ;; jswat --cp:p <classpath>
       (sh path "--cp:p" cp))))