(ns dewey.indexing
  "This is the logic for making changes to search index."
  (:require [clojurewerkz.elastisch.query :as es-query]
            [clojurewerkz.elastisch.rest.document :as es-doc]
            [clojurewerkz.elastisch.rest :as rest]
            [clojure-commons.file-utils :as file]
            [dewey.doc-prep :as prep]
            [dewey.config :as cfg]
            [dewey.entity :as entity]
            [qbits.spandex :as s])
  (:import
           [java.util Map]
           [clojure.lang Keyword]))

(def ^{:private true} collection-type "folder")
(def ^{:private true} data-object-type "file")

(defmulti ^{:private true} mapping-type-of type)

(defmethod mapping-type-of Map
  [entity]
  (mapping-type-of (entity/entity-type entity)))

(defmethod mapping-type-of Keyword
  [entity-type]
  (case entity-type
    :collection  collection-type
    :data-object data-object-type))

;; (defn- index-doc
;;   [es mapping-type doc]
;;   (es-doc/create es (cfg/es-index) mapping-type doc :id (str (:id doc))))

(defn- index-doc
;; (request client
;;          {:keys [method url headers query-string body keywordize?
;;                  response-consumer-factory exception-handler]
;;           :or {method :get
;;                keywordize? true
;;                exception-handler default-exception-handler
;;                response-consumer-factory
;;                HttpAsyncResponseConsumerFactory/DEFAULT}
;;           :as request-params}).

;; PUT https://<host>:<port>/<index-name>/_doc/<document-id>
;; {
;;   "title": "The Wind Rises",
;;   "release_date": "2013-07-20"
;; }
  [es doc]
  (let [index-name (cfg/es-index)
        url (str (cfg/es-uri) (format "/%s/_doc/%s" (cfg/es-index) (str (:id doc))))
        request-params {:method :put
                        :url url ;; (.toExternalForm url)
                        :headers {"Content-type" "application/json"}
                        :body doc
                        }]
    (s/request es request-params)))

(defn- update-doc
  "Scripted updates which are only compatible with Elasticsearch 5.x and greater."
  [es entity script params]
  (rest/post es
             (rest/record-update-url es
                                     (cfg/es-index)
                                     (mapping-type-of entity)
                                     (str (entity/id entity)))
             {:body {:script {:inline script :lang "painless" :params params}}}))

(defn entity-indexed?
  ([es entity]
   ^{:doc "Determines whether or not an iRODS entity has been indexed.

           Parameters:
             es     - the elasticsearch connection
             entity - the entity being checked

           Throws:
             This function can throw an exception if it can't connect to elasticsearch."}
   (es-doc/present? es (cfg/es-index) (mapping-type-of entity) (str (entity/id entity))))

  ([es entity-type entity-id]
   ^{:doc "Determines whether or not an iRODS entity has been indexed.

           Parameters:
             es          - the elasticsearch connection
             entity-type - :collection|:data-object
             entity-id   - the UUID of the entity being checked

           Throws:
             This function can throw an exception if it can't connect to elasticsearch."}
   (es-doc/present? es (cfg/es-index) (mapping-type-of entity-type) (str entity-id))))

(defn index-collection
  "Indexes a collection.

   Parameters:
     es    - the elasticsearch connection
     coll  - the collection entity to index

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. The
     function can also throw one if the collection is not in the iRODS data store."
  [es coll]
  (let [folder (prep/format-folder (entity/id coll)
                                   (entity/path coll)
                                   (entity/acl coll)
                                   (entity/creator coll)
                                   (entity/creation-time coll)
                                   (entity/modification-time coll)
                                   (entity/metadata coll))]
    ;; (index-doc es collection-type folder)
    (index-doc es folder)))

(defn index-data-object
  "Indexes a data object.

   Parameters:
     es        - the elasticsearch connection
     obj       - The CollectionAndDataObjectListingEntry of the data object to index
     creator   - (Optional) The username of the creator of the data object
     file-size - (Optional) The byte size of the data object
     file-type - (Optional) The media type of the data object

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. The
     function can also throw one if the data object is not in the iRODS data store."
  [es obj & {:keys [creator file-size file-type]}]
  (let [file (prep/format-file (entity/id obj)
                               (entity/path obj)
                               (entity/acl obj)
                               (or creator (entity/creator obj))
                               (entity/creation-time obj)
                               (entity/modification-time obj)
                               (entity/metadata obj)
                               (or file-size (entity/size obj))
                               (or file-type (entity/media-type obj)))]
    ;;(index-doc es data-object-type file)
    (index-doc es file)))

(defn remove-entity
  "Removes an iRODS entity from the search index.

   Parameters:
     es          - the elasticsearch connection
     entity-type - :collection|:data-object
     entity-id   - The UUID of the entity to remove

   Throws:
     This function can throw an exception if it can't connect to elasticsearch."
  [es entity-type entity-id]
  (when (entity-indexed? es entity-type entity-id)
    (es-doc/delete es (cfg/es-index) (mapping-type-of entity-type) (str entity-id))))

(defn remove-entities-like
  "Removes iRODS entities from the search index that have a path matching the provide glob. The glob
   supports * and ? wildcards with their typical meanings.

   This method uses the Elasticsearch 5.x Delete By Query API, and is not backward compatible with
   earlier versions of Elasticsearch.

   Parameters:
     es        - the elasticsearch connection
     path-glob - the glob describing the paths of the entities to remove

   Throws:
     This function can throw an exception if it can't connect to elasticsearch."
  [es path-glob]
  (rest/post es
             (rest/url-with-path es (cfg/es-index) "_delete_by_query")
             {:body {:query (es-query/wildcard :path path-glob)}}))

; XXX - I wish I could think of a way to cleanly and simply separate out the document update logic
; from the update scripts calls in the following functions. It really belongs with the rest of the
; document logic in the doc-prep namespace.

(defn update-path
  "Updates the path of an entity and optionally its modification time

   Parameters:
     es       - the elasticsearch connection
     entity   - the entity to update
     path     - the entity's new path
     mod-time - (Optional) the entity's modification time"
  ([es entity path]
   (update-doc es
               entity
               "ctx._source.path = params.path;
                ctx._source.label = params.label;"
               {:path  path
                :label (file/basename path)}))

  ([es entity path mod-time]
   (update-doc es
               entity
               "ctx._source.path = params.path;
                 ctx._source.label = params.label;
                 if (params.dateModified > ctx._source.dateModified) { ctx._source.dateModified = params.dateModified };"
               {:path         path
                :label        (file/basename path)
                :dateModified (prep/format-time mod-time)})))

(defn update-acl
  "Updates the indexed ACL of an entity.

   Parameters:
     es     - the elasticsearch connection
     entity - the entity whose ACL needs to be updated in elasticsearch

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. It can also
     throw if the entity has no index entry or is not in the iRODS data store."
  [es entity]
  (update-doc es
              entity
              "ctx._source.userPermissions = params.permissions"
              {:permissions (prep/format-acl (entity/acl entity))}))

(defn update-metadata
  "Updates the indexed AVU metadata of an entity.

   Parameters:
     es     - the elasticsearch connection
     entity - The entity whose metadata needs to be updated in elasticsearch

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. It can also
     throw if the entity has no index entry or is not in the iRODS data store."
  [es entity]
  (update-doc es
              entity
              "ctx._source.metadata = params.metadata"
              {:metadata (prep/format-metadata (entity/metadata entity))}))

(defn update-collection-modify-time
  "Updates the indexed modify time of a collection.

   Parameters:
     es   - the elasticsearch connection
     coll - the collection that was modified

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. It can
     also throw if the collection has no index entry or is not in the iRODS data store."
  [es coll]
  (update-doc es
              coll
              "if (params.dateModified > ctx._source.dateModified) { ctx._source.dateModified = params.dateModified } else { ctx.op = \"none\" }"
              {:dateModified (prep/format-time (entity/modification-time coll))}))

(defn update-data-object
  "Updates the indexed data object. It will update the modification time, file size and optionally
   file type for the data object.

   Parameters:
     es        - the elasticsearch connection
     obj       - the data object that was modified
     file-size - The data object's file size in bytes.
     file-type - (OPTIONAL) The media type of the data object.

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. It can
     also throw if the data object has no index entry or is not in the iRODS data store."
  ([es obj file-size]
   (update-doc es
               obj
               "if (params.dateModified > ctx._source.dateModified) { ctx._source.dateModified = params.dateModified; }
                ctx._source.fileSize = params.fileSize;"
               {:dateModified (prep/format-time (entity/modification-time obj))
                :fileSize     file-size}))

  ([es obj file-size file-type]
   (update-doc es
               obj
               "if (params.dateModified > ctx._source.dateModified) { ctx._source.dateModified = params.dateModified; }
                ctx._source.fileSize = params.fileSize;
                ctx._source.fileType = params.fileType;"
               {:dateModified (prep/format-time (entity/modification-time obj))
                :fileSize     file-size
                :fileType     file-type})))
