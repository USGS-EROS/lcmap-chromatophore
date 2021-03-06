(ns lcmap.clownfish.system
  (:require [again.core :as again]
            [clojure.tools.logging :as log]
            ;; requiring this ensures the server component will start
            [lcmap.clownfish.server :as server]
            [lcmap.clownfish.setup.cassandra]
            [mount.core :refer [defstate] :as mount]))

(defstate hook
  :start (do
           (log/debugf "registering shutdown handler")
           (.addShutdownHook (Runtime/getRuntime)
                             (Thread. #(mount/stop) "shutdown-handler"))))

(def default-retry-strategy
  (again/max-retries 10 (again/constant-strategy 5000)))

(defn start
  ([environment]
   (start environment default-retry-strategy))

  ([environment startup-retry-strategy]
   (again/with-retries startup-retry-strategy
     (do (log/info "Stopping mount components")
         (mount/stop)
         (log/info "Starting mount components...")
         ;; insulation to make sure these states do not get accidentally
         ;; started
         (mount/start-without #'lcmap.clownfish.setup.cassandra/setup
                              #'lcmap.clownfish.setup.cassandra/teardown
                              (mount/with-args {:environment environment}))))))
(defn stop []
  (try
    (mount/stop)
    (catch Exception ex
      (log/warnf "System did not shutdown cleanly: %s"  ex))))
