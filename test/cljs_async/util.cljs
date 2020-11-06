(ns cljs-async.util
  (:require [cljs.test :as t]))

(defmethod t/report [::silent :pass] [m]
  (t/inc-report-counter! :pass))

(defmethod t/report [::silent :fail] [m]
  (t/inc-report-counter! :fail))

(defmethod t/report [::silent :error] [m]
  (t/inc-report-counter! :error))

(defn intercept-report [f done]
  (let [restore! (let [prev (t/get-current-env)]
                   (fn []
                     (t/set-env! prev)))
        finish! (fn []
                  (let [result (t/get-current-env)]
                    (restore!)
                    (done (-> (:report-counters result)
                              (dissoc :test)))))]
    (t/set-env! (t/empty-env ::silent))
    (try
      (let [test (f)]
        (if (t/async? test)
          (test (fn []
                  (finish!)))
          (finish!)))
      (catch :default e
        (finish!)))))

(defn async-intercept-report [f rep]
  (t/async done
           (intercept-report f (fn [r]
                                 (try
                                   (rep r) ;; this must be synchronous
                                   (finally
                                     (done)))))))
