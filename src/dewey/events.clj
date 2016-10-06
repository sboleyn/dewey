(ns dewey.events
  (:require [clojure.tools.logging :as log]
            [langohr.basic :as lb]
            [dewey.config :as cfg])
  (:import [org.cyverse.events.ping PingMessages
                                    PingMessages$Ping
                                    PingMessages$Pong]
           [com.google.protobuf.util JsonFormat]))

(defn- ping-handler
  [channel routing-key msg]
  (log/info (format "[events/ping-handler] [%s] [%s]" routing-key msg))
  (lb/publish channel (cfg/amqp-events-exchange) "events.dewey.pong"
    (.print (JsonFormat/printer)
      (.. (PingMessages$Pong/newBuilder)
        (setPongFrom "dewey")
        (build)))))

(def handlers
  {"events.dewey.ping" ping-handler})

(defn consume-msg
  [channel routing-key msg]
  (let [handler (get handlers routing-key)]
    (if-not (nil? handler)
      (handler channel routing-key msg))))
