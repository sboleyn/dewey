(ns dewey.doc-prep-test
  (:use clojure.test
        dewey.doc-prep)
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]])
  (:import [java.util Date]
           [org.irods.jargon.core.protovalues FilePermissionEnum
                                              UserTypeEnum]
           [org.irods.jargon.core.pub.domain UserFilePermission]
           [org.irods.jargon.core.query CollectionAndDataObjectListingEntry]))


(defn- mk-acl
  []
  [(UserFilePermission. "name1" "name1" FilePermissionEnum/OWN UserTypeEnum/RODS_USER "zone1")
   (UserFilePermission. "name2" "name2" FilePermissionEnum/WRITE UserTypeEnum/RODS_USER "zone2")
   (UserFilePermission. "name3" "name3" FilePermissionEnum/READ UserTypeEnum/RODS_USER "zone3")
   (UserFilePermission. "name4" "name4" FilePermissionEnum/NULL UserTypeEnum/RODS_USER "zone4")])


(deftest format-acl-test
  (testing "Formats permissions correctly."
    (is (= (set (format-acl (mk-acl)))
           #{{:permission :own   :user "name1#zone1"}
             {:permission :write :user "name2#zone2"}
             {:permission :read  :user "name3#zone3"}}))))

(deftest format-time-test
  (testing "works for a java.util.Date object"
    (is (= (format-time (Date. 1386180216000)) 1386180216000)))
  (testing "works for a string containing a posix time in milliseconds"
    (is (= (format-time "1386180216000") 1386180216000))))

(defspec format-user-as-expected
         100
         (prop/for-all [u (gen/not-empty gen/string-alphanumeric)
                        z (gen/not-empty gen/string-alphanumeric)]
           (let [expected (str u \# z)]
             (and
               (= (format-user u z) expected)
               (= (format-user {:name u :zone z}) expected)))))
