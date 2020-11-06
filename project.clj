(defproject de.active-group/cljs-async "1.0-SNAPSHOT"
  :description "A ClojureScript library for asynchronous programming based on JavaScript promises."

  :url "http://github.com/active-group/cljs-async"
  
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src"]
  
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [org.clojure/clojurescript "1.10.773" :scope "provided"]
                 [unbroken-promises "0.1.9"]
                 ;; Note: this is the oldest version of core.async that does work.
                 [org.clojure/core.async "1.1.587"]]

  :plugins [[lein-codox "0.10.7"]
            [lein-auto "0.1.3"]]

  :profiles {
             ;; Note: codox does not run if shadow deps are included (in dev profile)
             :shadow [:dev {:resource-paths ["target"]
                            :source-paths ["src" "test"]
                            :dependencies [[thheller/shadow-cljs "2.11.1"]
                                           [binaryage/devtools "1.0.2"]]}]
             
             :codox {:dependencies [[codox-theme-rdash "0.1.2"]]}}

  :clean-targets ^{:protect false} [:target-path]

  :aliases {"dev" ["with-profile" "shadow" "run" "-m" "shadow.cljs.devtools.cli" "watch" "test"]}

  :codox {:language :clojurescript
          :metadata {:doc/format :markdown}
          :themes [:rdash]
          :src-dir-uri "http://github.com/active-group/cljs-async/blob/master/"
          :src-linenum-anchor-prefix "L"}

  :auto {:default {:paths ["src" "test"]}})
