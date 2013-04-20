(ns redial.test.handler
  (:require [clojure.java.jdbc :as sql]
            [cheshire.core :as json])
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

(defn assert-contains [url text
                       & {:keys [method params] :or {method :get, params {}}}]
  (let [response (app (request method url params))]
    (is (= (:status response) 200))
    (is (substring? text (:body response)))))

(deftest test-redial-routes
  (testing "main route"
    (assert-contains "/" "Welcome to Redial"))

  (testing "add route"
    (testing "get"
      (assert-contains "/add" "Add a URL"))

    (testing "post without URL"
      (assert-contains "/add" "no URL in post body" :method :post))

    (testing "post with valid url"
      (let [params {"url" "http://example.org"}
            response (app (request :post "/add" params))]
        (is (= (:status response) 200))
        (let [url ((json/parse-string (:body response)) "shortUrl")]
          (is (= (:id (get-url "url" (params "url")))
                 (Long/parseLong (second (re-find #"/(\w+)$" url)) 36))))))

    (testing "post with valid url on non-standard port"
      (let [dest-url "http://example.co.uk"
            params {"url" dest-url}
            response (app (assoc (request :post "/add" params)
                            :server-port 8001))]
        (is (= (:status response) 200))
        (is (.startsWith ((json/parse-string (:body response)) "shortUrl")
                         "http://localhost:8001"))))

    (testing "post with duplicate url returns error message"
      (let [params {"url" "http://example.us"}
            create-mock-request #(request :post "/add" params)
            response (and (app (create-mock-request))
                          (app (create-mock-request)))]
        (is (= (:status response) 200))
        (is (.startsWith ((json/parse-string (:body response)) "error")
                         "ERROR: duplicate key")))))

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

;; DB tests

(deftest test-add-url
  (let [url "http://example.com"]
    (add-url url)
    (sql/with-connection test-database-url
      (sql/with-query-results results
        ["SELECT * FROM urls WHERE url = ?" url]
        (is (= (:url (first results)) url))))))

(deftest test-add-duplicate-url
  (let [url "http://example.com"
        _ (add-url url)
        error (add-url url)]
    (is (re-find #"duplicate key value violates unique constraint"
                 (:error-message error)))
    (is (= (:sql-state error) "23505"))
    (is (instance? java.sql.SQLException (:original-exception error)))))

(deftest test-get-url-wth-update
  (let [url "http://example.com"
        id (:id (add-url url))
        found-url (get-url-with-update id)]
   (is (= found-url url))
   (get-url-with-update id)
   (is (= 2 (:visited (get-url id))))))
