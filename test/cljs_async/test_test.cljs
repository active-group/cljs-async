(ns cljs-async.test-test
  (:require [cljs.test :as t :refer (is testing deftest) :include-macros true]
            [cljs-async.core :as core :include-macros true]
            [cljs-async.test :as pt :include-macros true]
            [cljs-async.util :as u :include-macros true]))

(pt/deftest test-1
  (is false))

(u/de-test! test-1)

(deftest deftest-test-1
  (println "test")
  (u/async-intercept-report
   test-1
   (fn [report]
     (is (= {:fail 1 :error 0 :pass 0} report)))))

(pt/deftest test-2
  (is true)
  (core/promise
   (fn [resolve reject]
     (is true)
     (resolve true))))

(u/de-test! test-2)

(deftest deftest-test-2
  (u/async-intercept-report
   test-2
   (fn [report]
     (is (= {:fail 0 :error 0 :pass 2} report)))))

(pt/deftest test-3
  (core/async
   (core/await (core/promise
                (fn [resolve]
                  (is false)
                  (resolve nil))))
   (core/await
    (testing "Hello"
      (core/promise
       (fn [resolve reject]
         (is true)
         (resolve nil)))))))

(u/de-test! test-3)

(deftest deftest-test-3
  (u/async-intercept-report
   test-3
   (fn [report]
     (is (= {:fail 1 :error 0 :pass 1} report)))))

(pt/deftest test-4
  (core/async
   (core/await (core/promise
                (fn [_ reject]
                  (reject "Error"))))))

(u/de-test! test-4)

(deftest deftest-test-4
  (u/async-intercept-report
   test-4
   (fn [report]
     (is (= {:fail 0 :error 1 :pass 0} report)))))

(defn timeout
  [ms & [value]]
  (core/promise (fn [resolve _]
                  (js/setTimeout (partial resolve value) ms))))

(deftest async-fixture-test

  (let [steps (atom [])
        startup (fn []
                  (swap! steps conj :starting)
                  (-> (timeout 5)
                      (core/then (fn []
                                   (swap! steps conj :started)
                                   :foo))))

        cleanup (fn [r]
                  (is (= r :foo))
                  (swap! steps conj :cleaning)
                  (-> (timeout 5)
                      (core/then (fn []
                                   (swap! steps conj :cleaned)
                                   nil))))

        af (pt/async-fixture
            (fn [f]
              (core/async
               (let [v (core/await (startup))]
                 (core/await (f))
                 (core/await (cleanup v))))
              ;; or:
              #_(-> (startup)
                    (core/then #(f))
                    (core/then #(cleanup)))))]
    
    (is (contains? af :before))
    (is (contains? af :after))

    (t/async done
             ;; and now do what cljs.test will do:
             (((:before af))
              (fn []
                ;; before is now done
                ;; tests will run
                (swap! steps conj :testing)

                (((:after af))
                 (fn []
                   ;; after is now done
                   (is (= [:starting :started :testing :cleaning :cleaned]
                          @steps))
                   
                   (done))))))))

#_(deftest async-fixture-reference-test
  (let [started (atom 0)
        cleaned (atom 0)
        af (pt/async-fixture
            (fn [f]
              (-> (do (swap! started inc) (core/resolve nil))
                  (core/then #(f))
                  (core/then #(do (swap! cleaned inc) (core/resolve nil))))))]
    ;; using fixture twice should work:
    ;; Note: instead of failing, it will probably just hang.
    (t/async done
             (((:before af))
              (fn []
                (((:before af))
                 (fn []
                   (is (= 2 @started))
                   (is (= 0 @cleaned))
                   (((:after af))
                    (fn []
                      (((:after af))
                       (fn []
                         (is (= 2 @cleaned))
                         (done))))))))))))
