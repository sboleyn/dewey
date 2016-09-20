(ns dewey.util-test
  (:use clojure.test
        [midje.checking.core :only [extended-=]]
        dewey.util))

(def ^{:private true :const true} ordinaries
  "~!@#&`1234567890-=QWERTYUIOP}qwertyuiop]ASDFGHJKL:\"asdfghjkl;'
  ZXCVBNM<>zxcvbnm,/ ")

(deftest sql-glob->regex-test
  (testing "empty string maps to empty expression"
    (is (extended-= (sql-glob->regex "") #"")))
  (testing "leaves text that aren't special characters in SQL glob patterns or regular expressions
        alone."
    (is (extended-= (sql-glob->regex ordinaries) (re-pattern ordinaries))))
  (testing "_ maps to ."
    (is (extended-= (sql-glob->regex "_") #"."))
    (is (extended-= (sql-glob->regex "_a") #".a"))
    (is (extended-= (sql-glob->regex "a_") #"a."))
    (is (extended-= (sql-glob->regex "a_b") #"a.b"))
    (is (extended-= (sql-glob->regex "a_b_c") #"a.b.c"))
    (is (extended-= (sql-glob->regex "a__b") #"a..b")))
  (testing "% maps to .*"
    (is (extended-= (sql-glob->regex "%") #".*"))
    (is (extended-= (sql-glob->regex "%a") #".*a"))
    (is (extended-= (sql-glob->regex "a%") #"a.*"))
    (is (extended-= (sql-glob->regex "a%b") #"a.*b"))
    (is (extended-= (sql-glob->regex "a%b%c") #"a.*b.*c")))
  (testing "escaped characters don't get translated"
    (is (extended-= (sql-glob->regex "\\_") #"_"))
    (is (extended-= (sql-glob->regex "\\%") #"%"))
    (is (extended-= (sql-glob->regex "\\\\") #"\\"))
    (is (extended-= (sql-glob->regex "\\a") #"a")))
  (testing "special regex characters get escaped"
    (is (extended-= (sql-glob->regex "[") #"\["))
    (is (extended-= (sql-glob->regex ".") #"\."))
    (is (extended-= (sql-glob->regex "^") #"\^"))
    (is (extended-= (sql-glob->regex "$") #"\$"))
    (is (extended-= (sql-glob->regex "?") #"\?"))
    (is (extended-= (sql-glob->regex "*") #"\*"))
    (is (extended-= (sql-glob->regex "+") #"\+"))
    (is (extended-= (sql-glob->regex "{") #"\{"))
    (is (extended-= (sql-glob->regex "|") #"\|"))
    (is (extended-= (sql-glob->regex "(") #"\("))
    (is (extended-= (sql-glob->regex ")") #"\)"))))


(deftest get-parent-path-test
  (testing "returns parent without trailing slash"
    (is (extended-= (get-parent-path "/parent/child") "/parent"))))
