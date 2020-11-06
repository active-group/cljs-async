(ns cljs-async.core
  (:require [cljs.core.async :as a])
  (:refer-clojure :exclude [await]))

;; Note: This is based on clojure.core.async, but that should be
;; treated as an implementation detail. A different implementation in
;; the future could be more efficient, but not using the go macro is a
;; major piece of work.

(defmacro async
  "Returns a promise executing the code in `body`. Inside the
  body, [[async]] expressions can be used to wait for and bind to the
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
