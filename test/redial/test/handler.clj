(ns redial.test.handler
  (:require [clojure.java.jdbc :as sql])
  (:use clojure.test
        [ring.mock.request :only [request]]
        [redial handler db]))

(defn substring? [^String needle ^String haystack]
    (not (neg? (.indexOf haystack needle))))

(def test-database-url "postgres://localhost:5432/redial-test")

(defn db-fixture [f]
  (binding [database-url test-database-url]
    (create-tables)
    (f)
    (drop-tables)))

(use-fixtures :each db-fixture)

(defn assert-get-contains [url text]
  (let [response (app (request :get url))]
    (is (= (:status response) 200))
    (is (substring? text (:body response)))))

(deftest test-redial-routes
  (testing "main route"
    (assert-get-contains "/" "Welcome to Redial"))

  (testing "add route"
    (assert-get-contains "/add" "Add a new URL"))

  (testing "not-found route"
    (let [response (app (request :get "/bad"))]
      (is (= (:status response) 404))))

  (testing "found route"
    (let [row (add-url "http://example.org/")
          url (:url row)
          encoded-id (Long/toString (:id row) 36)
          short-url (str "/" encoded-id)
          response (app (request :get short-url))]
      (is (= (:status response) 302))
      (is (= (get (:headers response) "Location") url))
      (is (= (:visited (get-url (:id row))) 1)))))

(deftest test-add-url
  (let [url "http://example.com"]
    (add-url url)
    (sql/with-connection test-database-url
      (sql/with-query-results results
        ["SELECT * FROM urls WHERE url = ?" url]
        (is (= (:url (first results)) url))))))

(deftest test-get-url-wth-update
  (let [url "http://example.com"
        id (:id (add-url url))
        found-url (get-url-with-update id)]
   (is (= found-url url))
   (get-url-with-update id)
   (is (= 2 (:visited (get-url id))))))
