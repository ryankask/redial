(ns redial.db
  (:require [clojure.java.jdbc :as sql]
            [clj-time [core :as time-core] [coerce :as time-coerce]]))

(def ^:dynamic database-url (System/getenv "REDIAL_DATABASE_URL"))

(defmacro with-wrapped-sql-exception [& body]
  `(try
     ~@body
     (catch java.sql.SQLException e#
       {:error-message (.getMessage e#),
        :sql-state (.getSQLState e#),
        :original-exception e#})))

(defn create-tables []
  (sql/with-connection database-url
    (sql/create-table
     :urls
     [:id :serial "NOT NULL" "PRIMARY KEY"]
     [:url "varchar(2000)" "NOT NULL" "UNIQUE"]
     [:visited :bigint "NOT NULL" "DEFAULT 0"]
     [:created "timestamp with time zone" "NOT NULL"])))

(defn drop-tables []
  (sql/with-connection database-url
    (sql/transaction
     (try
       (sql/drop-table :urls)
       (catch Exception _)))))

(defn add-url [url]
  (sql/with-connection database-url
    (with-wrapped-sql-exception
      (sql/transaction
       (sql/insert-values
        :urls
        [:url :created]
        [url (time-coerce/to-timestamp (time-core/now))])))))

(defn get-url [id]
  (sql/with-connection database-url
    (sql/transaction
     (sql/with-query-results results
       ["SELECT * FROM urls WHERE id = ?" id]
       (first results)))))

(defn get-url-with-update [id]
  (sql/with-connection database-url
    (sql/transaction
     (sql/with-query-results results
       ["UPDATE urls SET visited = visited + 1 WHERE id = ? RETURNING *" id]
       (:url (first results))))))
