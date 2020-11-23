(ns cljs-async.core
  (:require [cljs.core.async :as a])
  (:refer-clojure :exclude [await with-redefs]))

;; Note: This is based on clojure.core.async, but that should be
;; treated as an implementation detail. A different implementation in
;; the future could be more efficient, but not using the go macro is a
;; major piece of work.

(defmacro async
  "Returns a promise executing the code in `body`. Inside the
  body, [[async]] expressions can be used to wait for and return the
  result of other promises. Errors thrown during the execution will
  result in a rejected promise.

  Example:
```clojure
  (async
    (let [r1 (await (some-async-op))]
      (or r1 (await (some-other-async-op)))))
```
"
  [& body]
  `(to-promise
    (a/go
      (try (let [r# (do ~@body)]
             (p-ch-success r#))
           (catch :default e#
             (p-ch-error e#))))))

(defmacro await
  "Return the value that the given promise resolves to, or throw the
  reason if it is rejected. This can only be used inside an [[async]]
  expression."
  [promise]
  `(let [v# (a/<! (from-promise ~promise))]
     (p-ch-result v#)))

(defmacro with-redefs
  "`binding => var-symbol temp-value-expr`

  Temporarily redefines vars while executing the body.  The
  temp-value-exprs will be evaluated and each resulting value will
  replace in parallel the root value of its var.  After the body is
  executed, the root values of all the vars will be set back to their
  old values. Useful for mocking out functions during testing.

  This is almost the same as [[clojure.core/with-redefs]], except if
  `body` evaluates to a promise, the vars will not be set back to
  their previous values until that promise settles."

  ;; same as https://github.com/clojure/clojurescript/blob/f884af0aef03147f3eef7a680579f704a7b6b81c/src/main/clojure/cljs/core.cljc#L2248
  ;; but try..finally replaced with core/try-finally
  [bindings & body]
  (let [names (take-nth 2 bindings)
        vals (take-nth 2 (drop 1 bindings))
        orig-val-syms (map (comp gensym #(str % "-orig-val__") name) names)
        temp-val-syms (map (comp gensym #(str % "-temp-val__") name) names)
        binds (map vector names temp-val-syms)
        resets (reverse (map vector names orig-val-syms))
        bind-value (fn [[k v]] (list 'set! k v))]
    `(let [~@(interleave orig-val-syms names)
           ~@(interleave temp-val-syms vals)]
       ~@(map bind-value binds)
       (try-finally
        (fn []
          ~@body)
        (fn []
          ~@(map bind-value resets))))))
