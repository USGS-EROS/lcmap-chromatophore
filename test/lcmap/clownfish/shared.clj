(ns lcmap.clownfish.shared
  (:require [again.core :as again]
            [clojure.edn :as edn]
            [clojure.stacktrace :as stacktrace]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [lcmap.clownfish.configuration :refer [config]]
            [lcmap.clownfish.event :as event]
            [lcmap.clownfish.setup.initialize :as initialize]
            [lcmap.clownfish.setup.finalize :as finalize]
            [lcmap.clownfish.system :as system]
            [mount.core :as mount]
            [org.httpkit.client :as http]))

(defmacro with-system
  "Start and stop the system, useful for integration tests."
  [& body]
  `(let [env#      (edn/read-string (slurp (io/resource "environment.edn")))
         strategy# (again/max-retries 1 (again/constant-strategy 5000))]
     (log/debugf "starting test system with environment: %s" env#)
     (try
       ;; start from a clean state every time
       (initialize/cassandra env#)
       (system/start env# strategy#)
       (catch Exception e#
         (log/errorf "Cannot start test system: %s" (stacktrace/root-cause e#))
         (System/exit 1)))
     (try
       ;; this seems like a really bad idea.  Refactor http-port
       ;; and http-host out of this macro body.  --dvh 2-14-17
       (let [~'http-port (get-in config [:http :port])
             ~'http-host (str "http://localhost:" ~'http-port)]
         ~@body)
       (finally
         (log/debug "Stopping test system")
         ;;; stop the listener then remove the rabbit items
         (mount/stop #'lcmap.clownfish.server/listener)
         (event/destroy-queue (get-in config [:server :queue]))
         (event/destroy-exchange (get-in config [:server :exchange]))
         (system/stop)
         (finalize/cassandra env#)))))

(defn req
  "Convenience function for making HTTP requests."
  [method url & {:keys [headers query-params form-params body]
                 :as   opts}]
  (let [defaults {:headers {"Accept" "application/json"}}]
    @(http/request (merge {:url url :method method} defaults opts))))
