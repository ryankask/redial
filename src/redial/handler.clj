(ns redial.handler
  (:require [redial.db :as db]
            [ring.util.response :as response]
            [ring.adapter.jetty :as jetty]
            [clojure.java.io :as io])
  (:use [ring.middleware.resource :only [wrap-resource]]
        [ring.middleware.file-info :only [wrap-file-info]]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.json :only [wrap-json-response]])
  (:import java.net.URL))

(def production? (System/getenv "LEIN_NO_DEV"))
(def cached-templates (atom {}))

(defn template-path [filename]
  (.getPath (io/file "templates" filename)))

(defn get-template [path]
  (or (and production? (@cached-templates path))
      (let [template (slurp (io/resource path))]
        (if production?
          (swap! cached-templates assoc path template))
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

(defn join-url [request code]
  (let [{:keys [scheme server-name server-port]} request
        host-part (if (or (= server-port 80) (= server-port 443))
                    (URL. (name scheme) server-name "")
                    (URL. (name scheme) server-name server-port ""))]
    (.toString (URL. host-part code))))

(defn handle-add [request]
  (let [form-params (:form-params request)]
    (if-let [url (form-params "url")]
      (let [row (db/add-url url)]
        (if-let [error-message (:error-message row)]
          (response/response {"error" error-message})
          (response/response
           {"shortUrl" (join-url request (Long/toString (:id row) 36))})))
      (response/response {"error" "No URL in post body"}))))

(defn add-url-form [request]
  (if (= (:request-method request) :post)
    (handle-add request)
    (render (get-template (template-path "add.html")))))

(defn redirect [uri]
  (try
    (let [id (Long/parseLong (apply str (rest uri)) 36)]
      (if-let [final-url (db/get-url-with-update id)]
        (response/redirect final-url)
        (not-found)))
    (catch NumberFormatException e
      (not-found))))

(defn handler [{:keys [uri] :as request}]
  (cond
   (= uri "/") (render (page "<h1>Welcome to Redial.</h1>"))
   (= uri "/add") (add-url-form request)
   (= uri "/favicon.ico") (not-found)
   :else (redirect uri)))

(def app
  (-> handler
      (wrap-resource "public")
      wrap-file-info
      wrap-params
      wrap-json-response))

(defn -main []
  (jetty/run-jetty app {:port 8080 :join? false}))
