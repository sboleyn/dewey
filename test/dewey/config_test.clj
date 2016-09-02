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
    (is (= (config/amqp-host) "rabbit"))
    (is (= (config/amqp-port) 5672))
    (is (= (config/amqp-user) "guest"))
    (is (= (config/amqp-pass) "guest"))
    (is (= (config/amqp-exchange) "de"))
    (is (true? (config/amqp-exchange-durable)))
    (is (false? (config/amqp-exchange-autodelete)))
    (is (= (config/amqp-qos) 100))
    (is (= (config/es-host) "elasticsearch"))
    (is (= (config/es-port) 9200))
    (is (= (config/irods-host) "irods"))
    (is (= (config/irods-port) 1247))
    (is (= (config/irods-zone) "iplant"))
    (is (= (config/irods-user) "rods"))
    (is (= (config/irods-pass) "notprod"))
    (is (= (config/irods-default-resource) ""))
    (is (= (config/irods-home) "/iplant/home"))
    (is (= (config/listen-port) 60000))))
