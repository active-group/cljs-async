(ns cljs-async.cljs.core-test
  (:require [cljs-async.test :refer (deftest) :include-macros true]
            [cljs.test :refer (is testing) :include-macros true]
            [cljs-async.core :refer (async await) :include-macros true]
            [cljs-async.cljs.core :as core]))

(deftest promise-test
  (async
   (testing "can be delivered immediately"
     (is (= 42 (await (core/async-deref (core/deliver (core/promise) 42))))))

   (testing "can be delivered later"
     (let [p (core/promise)]
       (js/setTimeout #(core/deliver p 42) 1)
       (is (= 42 (await (core/async-deref p))))))

   (testing "second deliver has no effect"
     (let [p (core/promise)]
       (core/deliver p 42)
       (core/deliver p 21)
       (is (= 42 (await (core/async-deref p))))))

   (testing "realized? works as expected"
     (let [p (core/promise)]
       (is (not (realized? p)))
       (core/deliver p 42)
       (is (realized? p))))

   (testing "async-deref with timeout works"
     (let [p (core/promise)]
       (js/setTimeout #(core/deliver p 42) 100)
       (is (= :timeout (await (core/async-deref p 1 :timeout)))))

     (let [p (core/promise)]
       (js/setTimeout #(core/deliver p 42) 1)
       (is (= 42 (await (core/async-deref p 100 :timeout))))))
   ))
