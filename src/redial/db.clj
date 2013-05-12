(ns redial.db
  (:require [clojure.java.jdbc :as sql]
            [clj-time [core :as time-core] [coerce :as time-coerce]]))

(def ^:dynamic db (System/getenv "REDIAL_DATABASE_URL"))

(defmacro with-wrapped-sql-exception [& body]
  `(try
     ~@body
     (catch java.sql.SQLException e#
       {:error-message (.getMessage e#),
        :sql-state (.getSQLState e#),
        :original-exception e#})))

(defn create-tables []
  (sql/with-connection db
    (sql/create-table
     :urls
     [:id :serial "NOT NULL" "PRIMARY KEY"]
     [:url "varchar(2000)" "NOT NULL" "UNIQUE"]
     [:visited :bigint "NOT NULL" "DEFAULT 0"]
     [:created "timestamp with time zone" "NOT NULL"])))

(defn drop-tables []
  (sql/with-connection db
    (sql/db-transaction [t-db db]
     (try
       (sql/drop-table :urls)
       (catch Exception _)))))

(defn add-url [url]
  (with-wrapped-sql-exception
    (sql/db-transaction [t-db db]
     (first
      (sql/insert!
       t-db :urls
       {:url url :created (time-coerce/to-timestamp (time-core/now))})))))

(defn get-url
  ([value] (get-url "id" value))
  ([column value]
     (first
      (sql/query
       db [(str "SELECT * FROM urls WHERE " column " = ?") value]))))

(defn get-url-with-update [id]
  (sql/db-transaction [t-db db]
    (:url (first
           (sql/query
            t-db
            ["UPDATE urls SET visited = visited + 1 WHERE id = ? RETURNING *" id])))))
