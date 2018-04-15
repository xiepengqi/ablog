(set-env!
 :source-paths #{"src" "debugmyself/posts/"}
 :resource-paths #{"theme" "test" "posts"}
 :dependencies '[[adzerk/boot-cljs "2.1.4"]
                 [pandeiro/boot-http "0.8.3"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [adzerk/boot-reload "0.5.2"]
                 [markdown-clj "1.0.2"]
                 [clj-time "0.14.2"]
                 [selmer "1.11.7"]
                 ;[adzerk/boot-test "1.2.0"]
                 ;[seancorfield/boot-expectations "1.0.11"]
                 [metosin/bat-test "0.4.0" :scope "test"]
                 [onetom/boot-lein-generate "0.1.3" :scope "test"]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[pandeiro.boot-http :refer [serve]]
         '[ablog.build :refer :all]
         ;'[adzerk.boot-test :refer :all]
         '[metosin.bat-test :refer (bat-test)])

(require 'boot.lein)
(boot.lein/generate)

(deftask parse
  "just a test"
  []
(generate))



(deftask dev
  "run the blog server"
  []
  (let [public-dir (:public-dir (get-settings))]
  (comp
    (serve :dir public-dir)
    (watch)
    (build)
    ;(cljs)
    ;(target :dir #{public-dir})
)))


