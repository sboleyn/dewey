(ns dewey.core
  (:gen-class)
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
            [qbits.spandex :as s]
            [clj-jargon.init :as irods]
            [clojure-commons.config :as config]
            [dewey.amq :as amq]
            [dewey.curation :as curation]
            [dewey.status :as status]
            [dewey.config :as cfg]
            [dewey.events :as events]
            [common-cli.core :as ccli]
            [me.raynes.fs :as fs]
            [service-logging.thread-context :as tc])
  (:import [java.net URL]
           [java.util Properties]))


(defn- init-es
  "Establishes a connection to elasticsearch"
  []
  (let [url      (URL. (cfg/es-uri))
        host-map {:hosts [(str url)]}
        opts     (if (or (empty? (cfg/es-user)) (empty? (cfg/es-password)))
                   host-map
                   (merge host-map {:http-client {:basic-auth
                                                  {:user (cfg/es-user)
                                                   :password (cfg/es-password)}}}))
        conn     (try
                   (s/client opts)
                   (catch Exception e (log/debug e) nil))]
    (if conn
      (do
        (log/info (format "Successfully connected to Elasticsearch: %s" url))
        conn)
      (do
        (log/info "Failed to find elasticsearch. Retrying...")
        (Thread/sleep 1000)
        (recur)))))


(defn- init-irods
  []
  (irods/init (cfg/irods-host)
              (str (cfg/irods-port))
              (cfg/irods-user)
              (cfg/irods-pass)
              (cfg/irods-home)
              (cfg/irods-zone)
              (cfg/irods-default-resource)))

(defn- connect-to-broker?
  [irods-cfg es]
  (try
    (amq/attach-to-exchange (cfg/amqp-uri)
                            (cfg/amqp-queue-name)
                            (cfg/amqp-exchange)
                            (cfg/amqp-exchange-durable)
                            (cfg/amqp-exchange-autodelete)
                            (cfg/amqp-qos)
                            (partial curation/consume-msg irods-cfg es)
                            "data-object.#"
                            "collection.#")
    (log/info (format "Attached to the AMQP broker. uri=%s exchange=%s queue=%s" (cfg/amqp-uri) (cfg/amqp-exchange) (cfg/amqp-queue-name)))
    true
    (catch Exception e
      (log/info e "Failed to attach to the AMQP broker. Retrying...")
      false)))


(defn- connect-to-events-broker?
  []
  (try
    (let [queue-name (str "events.dewey.queue." (cfg/environment-name))]
      (amq/attach-to-exchange (cfg/amqp-events-uri)
                              queue-name
                              (cfg/amqp-events-exchange)
                              (cfg/amqp-events-exchange-durable?)
                              (cfg/amqp-events-exchange-auto-delete?)
                              (cfg/amqp-qos)
                              events/consume-msg
                              "events.dewey.#")
      (log/info "Attached to the events AMQP broker on queue" queue-name)
      true)
    (catch Exception e
      (log/info e "Failed to attach to the evemts AMQP broker. Retrying...")
      false)))


(defn- listen
  [irods-cfg es]
  (let [attached? (connect-to-broker? irods-cfg es)]
    (when-not attached?
      (Thread/sleep 1000)
      (recur irods-cfg es))))


(defn- listen-for-events
  []
  (when-not (connect-to-events-broker?)
    (Thread/sleep 1000)
    (recur)))


(defn- listen-for-status
  []
  (.start
   (Thread.
     (partial status/start-jetty (cfg/listen-port)))))

(defn- run
  []
  (listen-for-status)
  (listen (init-irods) (init-es))
  (listen-for-events))


(def svc-info
  {:desc "Service that keeps an elasticsearch index synchronized with an iRODS repository."
   :app-name "dewey"
   :group-id "org.cyverse"
   :art-id "dewey"
   :service "dewey"})


(defn cli-options
  []
  [["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/dewey.properties"]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])


(defn -main
  [& args]
  (tc/with-logging-context svc-info
    (try+
     (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
       (when-not (fs/exists? (:config options))
         (ccli/exit 1 "The config file does not exist."))
       (when-not (fs/readable? (:config options))
         (ccli/exit 1 "The config file is not readable."))
       (cfg/load-config-from-file (:config options))
       (run))
      (catch Object _
        (log/error (:throwable &throw-context) "UNEXPECTED ERROR - EXITING")))))
