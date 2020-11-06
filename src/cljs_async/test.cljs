(ns cljs-async.test
  "Seemless asynchronous testing based on promises for cljs.test."
  (:require [cljs-async.core :as core :include-macros true]
            [cljs.test :as t :include-macros true]))



(defn simple-async-fixture
  "Returns an asynchronous 'map fixture' (see [[cljs.test/use-fixtures]]),
  defined by a single function `g`. When the fixture is applied, `g`
  will be called with a function `done`, which must eventually be
  applied when the setup is complete. The `done` can be called with a
  function that does the the tearing down of the fixture later. If
  such a function is passed it will later be called with a `done` too,
  which must be called eventually to complete the fixture.

  For example:

```
(simple-async-fixture
  (fn [init-done]
    (my-async-init
     (fn []
       (init-done (fn [finish-done]
                    (my-async-finish (fn []
                                       (finish-done)))))))))
```

  Note that the returned fixture cannot be used more than once; create
  fresh ones with the same function if you need it in more than one
  in the same test namespace.
"
  
  [g]
  ;; cljs.test should have offered/done this! :-/
  (let [res (atom nil)]
    {:before (fn []
               (t/async done
                        ;; the 'done' of g can be passed the 'after' fn.
                        (g (fn [after]
                             (reset! res [after])
                             (done)))))
     :after (fn []
              (t/async done
                       (if-let [[after] @res]
                         (after done)
                         ;; ...although, if 'done fn' above is never called, then cljs.test does not seem to call :after anyways (maybe add timeout?)
                         (throw (js/Error "Invalid async fixture: Passed function must be called eventually.")))))}))

;; Variant of async-fixture-0 that makes fixtures referentially
;; transparent (can be used multiple times; but uses global state for
;; that... hmmm; (maybe not that important for fixtures - keep it simple for now)
#_(let [active-fixtures
      (atom [])]
  (defn simple-async-fixture
    [g]
    ;; cljs.test should have offered/done this! :-/
    (let [id g]
      {:before (fn []
                 (t/async done
                          ;; the 'done' of g can be passed the 'after' fn.
                          (g (fn [after]
                               ;; Note: can have more than one active fixtures with same id.
                               (swap! active-fixtures conj [id after])
                               (done)))))
       :after (fn []
                (t/async done
                         (if-let [[_ after] (first (filter #(= id (first %))
                                                           @active-fixtures))]
                           (do (swap! active-fixtures
                                      ;; remove only the first with id
                                      (fn [l]
                                        (concat (take-while #(not= id (first %)) l)
                                                (rest (drop-while #(not= id (first %)) l)))))
                               (after done))
                           ;; ...although, if 'done fn' above is never called, then cljs.test does not seem to call :after anyways (maybe add timeout?)
                           (throw (js/Error "Invalid async fixture: Passed function must be called eventually.")))))})))

(defn async-fixture
  "Returns an asynchronous 'map fixture' (see [[cljs.test/use-fixtures]]),
  defined by a single function `g`. When the fixture is applied, `g`
  will be called with a function `f` and must return a promise. That
  promise can start with the required initializations, then must
  eventually call `f` and can bind cleanup code to the promise
  returned by `f` (which never fails).

  For example:

```
(async-fixture
  (fn [f]
    (-> (my-init)
        (then #(f))
        (then #(my-finish)))))
```

  Note that the returned fixture cannot be used more than once; create
  fresh ones with the same function if you need it in more than one
  in the same test namespace.
"
  [g]
  (simple-async-fixture
   (fn [done-init]
     (let [done-cleanup (atom nil)]
       (-> (g (fn the-tests []
                ;; returning a promise representing 'the tests', that will
                ;; resolve only when :after is called by cljs.test,
                ;; and should be 'scheduled' by user after
                ;; initialization promises.
                (core/promise
                 (fn [resolve reject]
                   (done-init (fn [f]
                                (resolve nil)
                                (reset! done-cleanup f)))))))
           (core/catch (fn [e]
                         ;; should not happen; can/should we make it a test error?
                         (throw (js/Error (str "Async fixture failed: " (or (.-message e) e))))))
           (core/then (fn the-cleanup [_]
                        (if-let [f @done-cleanup]
                          (f)
                          (throw (js/Error "Invalid async fixture: Passed function must be called eventually."))))))))))

(defn compose-async-fixtures
  "Composes two asynchronous 'map fixtures', creating an asychronous
  'map fixture' that combines their behavior."
  [f1 f2]
  (let [{b1 :before a1 :after} f1
        {b2 :before a2 :after} f2]
    {:before
     (fn []
       (t/async done
                ((b1) (fn b1-done []
                        ((b2) (fn b2-done []
                                (done)))))))
     :after
     (fn []
       (t/async done
                ((a1) (fn a1-done []
                        ((a2) (fn a2-done []
                                (done)))))))}))

(defn join-async-fixtures
  "Composes a collection of asynchronous 'map fixtures', in order."
  [fixtures]
  (reduce compose-async-fixtures fixtures))
