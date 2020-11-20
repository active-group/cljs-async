cljs-async
======

This is a library for asynchronous programming in ClojureScript based
on native JavaScript promises.

Besides basic functions to create and work with promises, which
correspond to the standardised [JavaScript Promise
API](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise),
this library contains an `async` macro that resembles async functions
in JavaScript, and functions that simplify writing asynchronous tests
with [`cljs.test`](https://cljs.github.io/api/cljs.test/).

The library is released on
[![Clojars](https://img.shields.io/clojars/v/de.active-group/cljs-async.svg)](https://clojars.org/de.active-group/cljs-async).
 
The generated API docs are available on
[cljdoc](https://cljdoc.xyz/d/de.active-group/cljs-async/CURRENT).

## Usage

Primitive promises can be created with `promise`:

```
(core/promise
  (fn [resolve reject]
     ...some asynchronous operation...
	 (resovle :result)))
```

To compose promises, the `async` macro is the most comfortable:

```
(core/async
  (let [v (core/await some-promise)
        w (core/await some-other-promise)]
    (* v w)))
```

## License

Copyright © 2020 Active Group GmbH

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
