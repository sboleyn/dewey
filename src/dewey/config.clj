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

(cc/defprop-optstr amqp-host
  "The hostname for the AMQP server"
  [props config-valid configs]
  "dewey.amqp.host" "rabbit")

(cc/defprop-optint amqp-port
  "The port number for the AMQP server"
  [props config-valid configs]
  "dewey.amqp.port" 5672)

(cc/defprop-optstr amqp-user
  "The username for the AMQP server"
  [props config-valid configs]
  "dewey.amqp.user" "guest")

(cc/defprop-optstr amqp-pass
  "The password for the AMQP user"
  [props config-valid configs]
  "dewey.amqp.password" "guest")

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

(cc/defprop-optstr es-host
  "The hostname for the Elasticsearch server"
  [props config-valid configs]
  "dewey.es.host" "elasticsearch")

(cc/defprop-optint es-port
  "The port number for the Elasticsearch server"
  [props config-valid configs]
  "dewey.es.port" 9200)

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
