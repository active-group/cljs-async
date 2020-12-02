(ns cljs-async.cljs.core
  (:refer-clojure :exclude [future]))

(defmacro future
  "Takes a body of expressions and yields a future object that will
  evaluate the body later, and will cache the result and return it on
  all subsequent calls to [[async-deref]]."
  [& body]
  `(cljs-async.cljs.core/future-call (fn [] ~@body)))
