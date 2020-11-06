(ns cljs-async.test
  (:require [unbroken-promises.macro :as ubp]
            [cljs-async.core :as core]
            [cljs.test :as t]))

(defmacro deftest
  "Like [[cljs.test/deftest]], but the last expression in `body` may
  be a promise which is then waited for.  A rejected promise is
  reported as a test failure. Tests defined by this are always a
  asynchronous tests."
  [name & body]
  `(t/deftest ~name
     (ubp/is-resolved [r# (core/lift (do ~@body))]
                      r#)))

#_(defmacro testing
  "Like [[cljs.test/testing]], but all expressions in `body` that
  evaluate to a promise are combined into a single
  promise (sequentially) and returned. So this can be used
  in [[deftest]] to label and combine multiple asynchronous test
  steps."
  [label & body]
  `(t/testing ~label
     (core/sequ-lift (list ~@body))))


#_(defmacro use-fixtures
  "Like [[cljs.test/testing]]"
  )
