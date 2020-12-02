(ns cljs-async.cljs.core
  "Promises and futures largely compatible with the clojure.core api."
  (:require [cljs-async.core :as core]))

;; Note: I think dynamic bindings, agents and probably refs, require
;; the notion of a 'current thread', which we don't have. (node.js has
;; 'Async hooks', which might be a way to follow)

;; --- async-deref ---
;; main difference to clojure, as JS does not have blocking.

(defprotocol IAsyncDeref
  (-async-deref [this] "Returns a js/Promise resolving to the value of
  this once it is available."))

(defprotocol IAsyncDerefWithTimeout
  (-async-deref-with-timeout [this msec timeout-val] "Returns a
  js/Promise resolving to the value of this once it is available, or
  to the given value after a timeout of the given number of
  milliseconds."))

(defn async-deref
  "Returns a js/Promise of the result of the given promise or future, optionally with a timeout."
  ([v]
   (cond
     (satisfies? IAsyncDeref v)
     (-async-deref v)

     :else
     (core/resolve (deref v))))
  ([v timeout-ms timeout-val]
   (cond
     (satisfies? IAsyncDerefWithTimeout v)
     (-async-deref-with-timeout v timeout-ms timeout-val)
     
     (satisfies? IAsyncDeref v)
     (core/race [(async-deref v)
                 (core/timeout timeout-ms timeout-val)])

     :else
     (core/resolve (-deref-with-timeout v timeout-ms timeout-val)))))

;; --- Promises ---

(deftype ^:private Promise [js resolve done?]
         IPending
         (-realized? [this]
           @done?)
         IAsyncDeref
         (-async-deref [this]
           js)
         IFn
         (-invoke [this v]
           (when-let [f @resolve]
             (f v)
             (reset! resolve nil)
             (reset! done? true))
           this))

(defn promise
  "Returns a promise object that can be read with [[async-deref]], and set,
  once only, with [[deliver]]. "
  []
  ;; Note: deliver might be called immediately, before promise was 'started'.
  (let [sync-res (atom nil)
        done? (atom false)
        res (atom (fn [v]
                    (reset! sync-res [v])))]
    (Promise. (core/promise (fn [resolve reject]
                              (if-let [[r] @sync-res]
                                (resolve r)
                                (reset! res resolve))))
              res
              done?)))

(defn deliver [p v]
  (assert (instance? Promise p))
  (p v))

;; --- Futures ---

(deftype ^:private Future [js done?]
         IPending
         (-realized? [this]
           @done?)
         IAsyncDeref
         (-async-deref [this]
           js))

(defn ^:no-doc future* [thunk]
  (let [done? (atom false)]
    (Future. (-> (core/promise (fn [resolve reject]
                                 (try (let [v (thunk)]
                                        ;; lift "async futures" implicitly (like all core fns do)
                                        (if (core/promise? v)
                                          (core/then v resolve reject)
                                          (resolve v)))
                                      (catch :default e
                                        (reject e)))))
                 (core/finally #(reset! done? true)))
             done?)))

