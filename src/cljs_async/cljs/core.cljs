(ns cljs-async.cljs.core
  "Promises and futures largely compatible with the clojure.core api.

  The main difference is, that [[cljs.core/deref]]/`@` cannot be used
  on promises and futures. Instead [[async-deref]] can be used to work
  with the respective results asynchronously. Also, futures may do
  asynchronous operations, by returning a [[core/promise]].

  Note that [[cljs.core/realized?]] works on both promises and futures."
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
     (core/race (async-deref v)
                (core/timeout timeout-ms timeout-val))

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
         ;; TODO? IPrintWithWriter
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

(defn deliver
  "Delivers the supplied value to the promise, releasing any pending
  derefs. A subsequent call to deliver on a promise will have no
  effect."
  [p v]
  (assert (instance? Promise p))
  (p v))

;; --- Futures ---

;; see future macro in core.clj

(deftype ^:private Future [js done?]
         IPending
         (-realized? [this]
           @done?)
         ;; TODO? IPrintWithWriter
         IAsyncDeref
         (-async-deref [this]
           js))

(defn future-call
  "Takes a function of no args and yields a future object that will
  invoke the function later, and will cache the result and return it on
  all subsequent calls to [[async-deref]]."
  [thunk]
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

(defn future?
  "Returns true if `v` is a future."
  [v]
  (instance? Future v))

(defn future-cancel
  "Cancels the future, if possible."
  [f]
  ;; Never possible, afaik.
  false)

(defn future-cancelled?
  "Returns true if the given future is cancelled."
  [f]
  ;; see future-cancel
  false)

(defn future-done?
  "Returns true if the given future is done."
  [f]
  ;; or cancelled, if we could cancel...
  (realized? f))
