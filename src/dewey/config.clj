(ns dewey.config
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.config :as cc]
            [clojure-commons.error-codes :as ce]))

(def ^:private props (ref nil))
(def ^:private config-valid (ref true))
(def ^:private configs (ref []))

(cc/defprop-optstr environment-name
  "The name of the deployment environment this is part of."
  [props config-valid configs]
  "dewey.environment-name" "docker-compose")

(cc/defprop-optstr amqp-events-uri
  "The URI for the handling event messages"
  [props config-valid configs]
  "dewey.events.amqp.uri" "amqp://guest:guest@rabbit:5672/%2F")

(cc/defprop-optstr amqp-events-exchange
  "The name of the exchange to connect to for event processing"
  [props config-valid configs]
  "dewey.events.amqp.exchange" "de")

(cc/defprop-optstr amqp-events-exchange-type
  "The type of the exchange"
  [props config-valid configs]
  "dewey.events.amqp.exchange.type" "topic")

(cc/defprop-optboolean amqp-events-exchange-durable?
  "Whether or not the exchange is durable"
  [props config-valid configs]
  "dewey.events.amqp.exchange.durable" true)

(cc/defprop-optboolean amqp-events-exchange-auto-delete?
  "Whether or not to delete the exchange on disconnection"
  [props config-valid configs]
  "dewey.events.amqp.exchange.auto-delete" false)

(cc/defprop-optstr amqp-uri
  "The URI for the main AMQP broker"
  [props config-valid configs]
  "dewey.amqp.uri" "amqp://guest:guest@rabbit:5672/%2F")

(cc/defprop-optstr amqp-exchange
  "The exchange name for the AMQP server"
  [props config-valid configs]
  "dewey.amqp.exchange.name" "de")

(cc/defprop-optboolean amqp-exchange-durable
  "Whether the AMQP exchange is durable"
  [props config-valid configs]
  "dewey.amqp.exchange.durable" true)

(cc/defprop-optboolean amqp-exchange-autodelete
  "Whether the AMQP exchange is auto-delete"
  [props config-valid configs]
  "dewey.amqp.exchange.auto-delete" false)

(cc/defprop-optint amqp-qos
  "How many messages to prefetch from the AMQP queue."
  [props config-valid configs]
  "dewey.amqp.qos" 100)

(cc/defprop-optstr amqp-queue-name
  "The AMQP queue name for AMQP operations."
  [props config-valid configs]
  "dewey.amqp.queue_name"  "dewey.indexing")

(cc/defprop-optstr es-uri
  "The hostname for the Elasticsearch server"
  [props config-valid configs]
  "dewey.es.uri" "http://elasticsearch:9200")

(cc/defprop-optstr es-user
  "The username for the Elasticsearch server"
  [props config-valid configs]
  "dewey.es.username" "admin")

(cc/defprop-optstr es-password
  "The password for the Elasticsearch server"
  [props config-valid configs]
  "dewey.es.password" "admin")

(cc/defprop-optstr es-index
  "The Elasticsearch index"
  [props config-valid configs]
  "dewey.es.index" "data")

(cc/defprop-optstr irods-host
  "The hostname for the iRODS server"
  [props config-valid configs]
  "dewey.irods.host" "irods")

(cc/defprop-optint irods-port
  "The port number for the iRODS server"
  [props config-valid configs]
  "dewey.irods.port" 1247)

(cc/defprop-optstr irods-zone
  "The zone name for the iRODS server"
  [props config-valid configs]
  "dewey.irods.zone" "iplant")

(cc/defprop-optstr irods-user
  "The username for the iRODS server"
  [props config-valid configs]
  "dewey.irods.user" "rods")

(cc/defprop-optstr irods-pass
  "The password for the iRODS user"
  [props config-valid configs]
  "dewey.irods.password" "notprod")

(cc/defprop-optstr irods-default-resource
  "The default resource to use with the iRODS server. Probably blank."
  [props config-valid configs]
  "dewey.irods.default-resource" "")

(cc/defprop-optstr irods-home
  "The base home directory for the iRODS server."
  [props config-valid configs]
  "dewey.irods.home" "/iplant/home")

(cc/defprop-optint listen-port
  "The port number to listen on for status requests."
  [props config-valid configs]
  "dewey.status.listen-port" 60000)

(defn- validate-config
  []
  (when-not (cc/validate-config configs config-valid)
    (throw+ {:error_code ce/ERR_CONFIG_INVALID})))

(defn load-config-from-file
  [cfg-path]
  (cc/load-config-from-file cfg-path props)
  (cc/log-config props :filters [#"(irods|amqp)\.(user|pass)"])
  (validate-config))
