(ns cljs-async.core-test
  (:require [cljs.test :as t :include-macros true]
            [cljs-async.core :as core :include-macros true]))

(t/deftest async-test-1
  (t/async done
           (let [a (core/async 42)]
             (t/is (core/promise? a))
             (-> a
                 (core/then (fn [v]
                              (t/is (= 42 v))))
                 (core/catch (fn [e]
                               (t/is false e)))
                 (core/finally done)))))

(t/deftest async-test-2
  (t/async done
           (let [a (core/async (throw "Test"))]
             (t/is (core/promise? a))
             (-> a
                 (core/then (fn [v]
                              (t/is false v)))
                 (core/catch (fn [e]
                               (t/is (= "Test" e))))
                 (core/finally done)))))

(t/deftest ifn-test
  (t/async done
           (-> (core/resolve {:a 42})
               (core/then :a)
               (core/then (fn [v]
                            (t/is (= v 42))))
               (core/catch (fn [e]
                             (t/is false)))
               (core/finally done))))

(t/deftest async-await-test-1
  (t/async done
           (let [a (core/async
                    (let [x (core/await (core/resolve 21))]
                      (* x 2)))]
             (-> a
                 (core/then (fn [v]
                              (t/is (= 42 v))))
                 (core/finally done)))))

(t/deftest async-await-test-2
  (t/async done
           (let [a (core/async
                    (let [x (core/await
                             (core/async (throw "Test")))]
                      (* x 2)))]
             (-> a
                 (core/then (fn [v]
                              (t/is false v)))
                 (core/catch (fn [v]
                               (t/is (= "Test" v))))
                 (core/finally done)))))

(t/deftest async-await-test-3
  (t/async done
           (let [a (core/async
                    (let [x (core/await
                             (core/reject "Test"))]
                      (* x 2)))]
             (-> a
                 (core/then (fn [v]
                              (t/is false v)))
                 (core/catch (fn [v]
                               (t/is (= "Test" v))))
                 (core/finally done)))))

(t/deftest all-settled-test
  (t/async done
           (let [a1 (core/resolve "Ok")
                 a2 (core/reject "Fail")]
             (-> (core/all-settled (list a1 a2))
                 (core/then (fn [res]
                              (t/is (= res [{:status "fulfilled" :value "Ok"}
                                            {:status "rejected" :reason "Fail"}]))))
                 (core/catch (fn [v]
                               (t/is false v)))
                 (core/finally done)))))
