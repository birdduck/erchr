(ns erchr.macros
  (:require
    [clojure.data.csv :as csv]
    [clojure.java.io :as io]))

(defn csv-data->classes [csv-data]
  (mapv zipmap
        (->> (first csv-data)
             (map keyword)
             repeat)
        (rest csv-data)))

(defmacro read-classes []
  (let [file (io/resource "classes.csv")]
    (with-open [reader (io/reader file)]
      (csv-data->classes (csv/read-csv reader)))))

(defn csv-data->stat [csv-data]
  (mapv zipmap
        (->> (first csv-data)
             (map keyword)
             repeat)
        (rest csv-data)))

(defmacro read-stat [stat-file]
  (let [file (io/resource stat-file)]
    (with-open [reader (io/reader file)]
      (csv-data->stat (csv/read-csv reader)))))
