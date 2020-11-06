(ns cljs-async.util)

(defmacro de-test!
  "Reset `name` to just the test function, removing it from the 'real' list of tests."
  [name]
  `(let [r# (:test (meta (var ~name)))]
     (set! ~name r#)))
