(ns cljs-async.core
  "Core functionality to do asynchronous programming."
  (:require [cljs.core.async :as async :include-macros true]
            [goog.object :as gobject]
            unbroken-promises.data)
  (:refer-clojure :exclude [resolve]))

(let [call (fn [f & args]
             (apply f args))]
  (defn- lift-ifn [f]
    (if (and (ifn? f) (not (fn? f)))
      (.bind call nil f)
      f)))

(defn promise
  "Creates a new promise based on a function `f`, which is later
  called with two arguments `resolve` and `reject`. Eventually one of
  those functions must be called."
  [f]
  (new js/Promise (lift-ifn f)))

(defn promise?
  "Returns whether v is a promise."
  [v]
  (instance? js/Promise v))

(defn resolve
  "Returns a promise that is immediately in the resolved state, with
  `v` as its result, unless `v` is a promise, in which case `v` is
  returned."
  [v]
  (js/Promise.resolve v))

(defn reject
  "Returns a promise that is immediately in the rejected state, with
  the given `reason` value, which is usually an error value."
  [reason]
  (js/Promise.reject reason))

(defn then
  "Returns a promise, that continues in the function `f` once and if
  the promise `p` is resolved. `f` will be called with the result of
  `p` and may return another promise. Passing `catch-f` is the same as
  using [[catch]] on the returned promise."
  ([p f]
   (.then p (lift-ifn f)))
  ([p f catch-f]
   (.then p (lift-ifn f) (lift-ifn catch-f))))

(defn catch
  "Returns a promise, that continues in the function `f` once and if
  the promise `p` is rejected. `f` will be called with the reason of
  the rejected `p` and may return another promise."
  [p f]
  (.catch p (lift-ifn f)))

(defn finally
  "Returns a promise, that calls `f` once the promise `p` is settled,
  no matter if resolved or rejected. `f` will be called with no
  arguments, and its return value is ignored."
  [p f]
  (.finally p (lift-ifn f)))

(defn all
  "Returns a promise that resolves to a sequence of the results of all
  the given promises, or rejects with the first promise in the list
  that is rejected."
  [promises]
  (-> (js/Promise.all promises)
      (then array-seq)))

(defn any
  "Returns a promise that resolves to the any of the given promises
  which resolves successfully. It rejects only if all the given
  promises are rejects, in which case the reason will be a
  js/AggregateError containing all the rejection reasons."
  [promises]
  (-> (js/Promise.any promises)
      (then array-seq)))

(defn race
  "Returns a promise that resolves or rejects to the first promise
  from the list of promises that does any of that."
  [promises]
  (-> (js/Promise.race promises)
      (then array-seq)))

(let [settled-res (fn [a]
                    (->> (array-seq a)
                         (map (fn [x]
                                ;; x should have a .status and either a .value or a .reason
                                (persistent!
                                 (reduce (fn [r k] (assoc! r (keyword k)
                                                           (gobject/get x k)))
                                         (transient {})
                                         (js-keys x)))))))]
  (defn all-settled
    "Returns a promise that resolves to a sequence of infos for each
  of the given promises, once all of them have settled. Those
  informative objects are maps with a `:status` entry, and either a
  `:value` entry for resolved promises, or a `:reason` entry for those
  that are rejected."
    [promises]
    (-> (js/Promise.allSettled promises)
        (then settled-res))))

(defn sequ
  "Returns a promise that will run the given promises sequentially in
  the given order, resolving to the value of the last one, or
  rejecting immediately if any of them is rejected."
  [promises]
  (reduce then
          (resolve nil)
          (map constantly promises)))

(defn ^:no-doc lift [v]
  (if (promise? v)
    v
    (resolve v)))

#_(defn timeout
  "Returns a promise that resolves after the given number of
  milliseconds to the given value, which defaults to `nil`."
  [ms & [value]]
  (promise (fn [resolve _]
             (js/setTimeout (partial resolve value) ms))))

;; async-await support fns:
;; Note: There is cljs.core.async.interop for something very similar, but that wraps errors in an ex-info.

(defrecord ^:private Error [value])

(defn ^:no-doc p-ch-error [v]
  (Error. v))

(defn ^:no-doc p-ch-success [v]
  v)

(defn ^:no-doc p-ch-result [v]
  (if (instance? Error v)
    (throw (:value v))
    v))

(defn ^:no-doc to-promise [p-ch]
  (new js/Promise
       (fn [resolve reject]
         (async/go
           (let [v (async/<! p-ch)]
             (if (instance? Error v)
               (reject (:value v))
               (resolve v)))))))

(defn ^:no-doc from-promise [p]
  (let [c (async/promise-chan)]
    (-> p
        (then (fn [res]
                (if (nil? res)
                  (async/close! c)
                  (async/put! c res))))
        (catch (fn [err]
                 (async/put! c (Error. err)))))
    c))
