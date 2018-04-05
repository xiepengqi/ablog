(ns ablog.build
  (:require [markdown.core :as md]
    [clojure.java.io :as io]
    [clj-time.format :as clj-time-format]
    [clj-time.core :as clj-time-core]
    [selmer.parser :refer [render render-file]]))

(selmer.parser/set-resource-path! (System/getProperty "user.dir"))



; 默认网站参数
(def default-settings {
  :site-title "a git blog"
  :posts-dir "posts"
  :page-dir "pages"
  :public-dir "public"
  :valid-filename-ext #{"md" "html" "gdb"}
  :theme "default"
  :post-date-format "yyyy-MM-dd HH:mm"
  :post-filename-date-format "yyyyMMddHHmm"
  :post-permalink ":year/:month/:day/:title/"
})

; 获取网站参数
(defn get-settings
  "获取网站的各项设置"
  []
  (merge default-settings (read-string (slurp "settings.ini"))))


(defn get-file-ext 
  "获取文件后缀名"
  [filename]
  (->> filename
    (#(.lastIndexOf % "."))
    (#(inc %))
    (subs filename)))

(defn is-valid-file
  "是否是有效的模板文件，返回模板后缀名"
  [settings file]
  (get (:valid-filename-ext settings) (get-file-ext (str file))))

; 时间输入格式
(def multi-parser (clj-time-format/formatter (clj-time-core/default-time-zone) "yyyy-MM-dd" "yyyy/MM/dd" "dd/MM/yyyy" "yyyy-MM-dd HH:mm" "yyyy/MM/dd HH:mm" "dd/MM/yyyy HH:mm" "yyyyMMdd HH:mm" "yyyyMMddHHmm" "yyyyMMdd" "yyyyMMddHH"))


(defn time-formater
  [time-str]
  (clj-time-format/parse multi-parser time-str))


(defn get-post-date
  "获取 post 提交时间"
  [settings post-config file]
  (if-let [write-date (:date post-config)]
    (time-formater write-date)
    (->> (re-find #"^(\d+)-(.*?)\.md$" (.getName file))
        (second)
        (time-formater)
    )))

(defn get-post-url
  "获取 post 的对外访问的 url"
  [settings post-filepath]
  (subs post-filepath (count (:public-dir settings))))




(defn get-public-post-filepath
  "获取源文件文件对应的public文件"
  [settings file post-time]
  (str (:public-dir settings)
       "/" 
       (let [f (reduce (fn [s [key val]] (clojure.string/replace s (re-pattern (str key)) (str val))) 
        (:post-permalink settings) {:year (str (clj-time-core/year post-time)) 
          :month (str (clj-time-core/month post-time)) 
          :day (str (clj-time-core/day post-time)) 
          :title (.getName file)})]
          (subs f 0 (clojure.string/last-index-of f "."))
       )
       ".html"))



(defn parse-post
  "获取post的相关值"
  [settings file]
  (if (is-valid-file settings file)
    (let [rdr (clojure.java.io/reader file)
          post-config (read (java.io.PushbackReader. rdr))
          post-content (line-seq (java.io.BufferedReader. rdr))
          post-date (get-post-date settings post-config file)
          post-filepath (get-public-post-filepath settings file post-date)
          post-url (get-post-url settings post-filepath)
          ]
          {:content post-content :date post-date :filepath post-filepath :url post-url 
          :title (:title post-config)}
      )
))

(defn get-posts-list
  "获取所有的post文件列表，返回的是：
  文章内容
  文章标题
  文章时间
  其它变量的一个map组成的列表"
  [settings]
  ; (doall
  (->> (file-seq (clojure.java.io/file (:posts-dir settings)))
        (pmap #(parse-post settings %))
        (filter not-empty)
        (sort-by :date)
  ))
  ; )





(defn generate-html
  "为相应的模板生成页面"
  [settings [prev-post post next-post] template_filename]
  (let [new-post (assoc post :prev-post prev-post :next-post next-post) 
    post-html (render-file (str "theme/" (:theme settings) "/" template_filename) new-post)]
    (println template_filename)
    ; (println post-html)
    (println (:filepath post))
    (clojure.java.io/make-parents (:filepath post))
    (spit (:filepath post) post-html))
)

(defn generate-homepage
  "生成首页"
  [settings [prev-post post next-post]]
  (println (assoc post :filepath "index.html"))
  (generate-html settings [prev-post (assoc post :filepath (str (:public-dir settings) "/index.html")) next-post] "index.html"))

(defn generate
  "整站生成静态网站"
  []
  (let [settings (get-settings)
        post-part-list (partition 3 1 (lazy-cat [nil] (get-posts-list settings) [nil]))
        newest-post-part (last post-part-list)
    ]
      (generate-homepage settings newest-post-part)
      (pmap #(generate-html settings % "post.html") post-part-list)
    ))

