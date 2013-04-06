(ns redial.handler
  (:require [redial.db :as db]
            [ring.util.response :as response]
            [ring.adapter.jetty :as jetty]
            [clojure.java.io :as io]))

(def cached-templates (atom {}))

(defn template-path [filename]
  (.getPath (io/file "templates" filename)))

(defn get-template [path]
  (or (@cached-templates path)
      (let [template (slurp (io/resource path))]
        (swap! cached-templates assoc path template)
        template)))

(defn page [content]
  (str "<!DOCTYPE html>"
       "<html>"
       "<head><title>Hello, world!</title></head>"
       "<body>" content "</body>"
       "</html>"))

(defn not-found []
  (response/not-found (page "<h1>404 - Not found</h1>")))

(defn render [body]
  (-> (response/response body)
      (response/content-type "text/html")))

(defn add-url-form [request]
  (if (= (:request-method request) :post)
    (render (page "would post"))
    (render (get-template (template-path "add.html")))))

(defn redirect [uri]
  (let [id (Long/parseLong (apply str (rest uri)) 36)]
    (if-let [final-url (db/get-url-with-update id)]
      (response/redirect final-url)
      (not-found))))

(defn handler [{:keys [uri] :as request}]
  (cond
   (= uri "/") (render (page "<h1>Welcome to Redial.</h1>"))
   (= uri "/add") (add-url-form request)
   (= uri "/favicon.ico") (not-found)
   :else (redirect uri)))

(def app handler)

(defn -main []
  (jetty/run-jetty app {:port 8080 :join? false}))
