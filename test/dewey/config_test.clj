(ns dewey.config-test
  (:use [clojure.test])
  (:require [dewey.config :as config]))

(defn with-config-defaults [f]
  (require 'dewey.config :reload)
  (config/load-config-from-file "dev-resources/empty.properties")
  (f))

(use-fixtures :once with-config-defaults)

(deftest test-default-config
  (testing "default configuration settings"
    (is (= (config/environment-name) "docker-compose"))
    (is (= (config/amqp-uri) "amqp://guest:guest@rabbit:5672/%2F"))
    (is (= (config/amqp-events-uri) "amqp://guest:guest@rabbit:5672/%2F"))
    (is (= (config/amqp-events-exchange) "de"))
    (is (= (config/amqp-events-exchange-type) "topic"))
    (is (true? (config/amqp-events-exchange-durable?)))
    (is (false? (config/amqp-events-exchange-auto-delete?)))
    (is (= (config/amqp-exchange) "de"))
    (is (true? (config/amqp-exchange-durable)))
    (is (false? (config/amqp-exchange-autodelete)))
    (is (= (config/amqp-qos) 100))
    (is (= (config/es-uri) "http://elasticsearch:9200"))
    (is (= (config/irods-host) "irods"))
    (is (= (config/irods-port) 1247))
    (is (= (config/irods-zone) "iplant"))
    (is (= (config/irods-user) "rods"))
    (is (= (config/irods-pass) "notprod"))
    (is (= (config/irods-default-resource) ""))
    (is (= (config/irods-home) "/iplant/home"))
    (is (= (config/listen-port) 60000))))
