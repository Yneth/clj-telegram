(defproject clj-telegram "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]]
  :repl-options {:init-ns clj-telegram.core}
  :profiles {:provided
             {:dependencies
              [[org.clojure/data.json "1.0.0"]
               [http-kit "2.5.0"]]}})