(ns cats.monad.writer-spec
  #?(:cljs
     (:require [cljs.test :as t]
               [cats.builtin :as b]
               [cats.protocols :as p]
               [cats.data :as d]
               [cats.monad.writer :as writer]
               [cats.monad.maybe :as maybe]
               [cats.core :as m :include-macros true])
     :clj
     (:require [clojure.test :as t]
               [cats.builtin :as b]
               [cats.protocols :as p]
               [cats.data :as d]
               [cats.monad.writer :as writer]
               [cats.monad.maybe :as maybe]
               [cats.core :as m])))

(t/deftest writer-monad-tests
  ;; Putting a value in a writer context yields an empty log
  (t/is (= 42 (writer/value (p/mreturn writer/writer-monad 42))))
  (t/is (= [] (writer/log (p/mreturn writer/writer-monad 42))))

  ;; The `tell` function adds the given value to the log
  (t/is (= ["Hello" "world"]
           (m/with-monad writer/writer-monad
             (writer/log (m/>> (writer/tell "Hello")
                               (writer/tell "world"))))))

  ;; The `listen` function yields a pair with the value and the log
  (let [w (m/with-monad writer/writer-monad
            (m/>> (writer/tell "Hello")
                  (writer/tell "world")
                  (m/return 42)))
        w (writer/listen w)]
    (t/is (= (d/pair 42 ["Hello" "world"])
             (first w)))
    (t/is (= ["Hello" "world"]
             (second w))))

  ;; The `pass` function can be used to apply a function to the log
  (let [w (m/with-monad writer/writer-monad
            (m/>> (writer/tell "Hello")
                  (writer/tell "world")
                  (m/return [42 reverse])))
        w (writer/listen (writer/pass w))]
    (t/is (= (d/pair 42 ["world" "Hello"])
             (first w)))
    (t/is (= ["world" "Hello"]
             (second w))))

  ;; The log values are accumulated using Semigroup's `mappend` operation
  (let [w (m/with-monad writer/writer-monad
            (m/>> (writer/pass (m/return [42 set]))
                  (writer/tell "Hello")
                  (writer/tell "Hello")
                  (writer/tell "world")
                  (writer/tell "world")
                  (writer/tell "Hello")
                  (writer/tell "world")))
        w (writer/listen w)]
    (t/is (= #{"world" "Hello"}
             (second w)))))


(def writer-maybe-t (writer/writer-transformer maybe/maybe-monad))

(t/deftest writer-transformer-tests
  ;; Putting a value in a writer transformer context yields an empty log
  (let [w (m/with-monad writer-maybe-t
            (m/return 42))]
    (t/is (= 42 (writer/value (maybe/from-maybe w))))
    (t/is (= [] (writer/log (maybe/from-maybe w)))))

  ;; The `tell` function adds the given value to the log
  (let [w (m/with-monad writer-maybe-t
            (m/>> (writer/tell "Hello")
                  (writer/tell "world")))]
    (t/is (= ["Hello" "world"]
             (writer/log (maybe/from-maybe w)))))

  ;; The `listen` function yields a pair with the value and the log
  (let [w (m/with-monad writer-maybe-t
            (m/>> (writer/tell "Hello")
                  (writer/tell "world")
                  (m/return 42)))
        w (m/with-monad writer-maybe-t
            (writer/listen w))]
    (t/is (= (d/pair 42 ["Hello" "world"])
             (first (maybe/from-maybe w))))
    (t/is (= ["Hello" "world"]
             (second (maybe/from-maybe w)))))

  ;; The `pass` function can be used to apply a function to the log
  (let [w (m/with-monad writer-maybe-t
            (m/>> (writer/tell "Hello")
                  (writer/tell "world")
                  (m/return [42 reverse])))
        w (m/with-monad writer-maybe-t
            (writer/listen (writer/pass w)))]
    (t/is (= (d/pair 42 ["world" "Hello"])
             (first (maybe/from-maybe w))))
    (t/is (= ["world" "Hello"]
             (second (maybe/from-maybe w)))))

  ;; The log values are accumulated using Semigroup's `mappend` operation
  (let [w (m/with-monad writer-maybe-t
            (m/>> (writer/pass (m/return [42 set]))
                  (writer/tell "Hello")
                  (writer/tell "Hello")
                  (writer/tell "world")
                  (writer/tell "world")
                  (writer/tell "Hello")
                  (writer/tell "world")))
        w (m/with-monad writer-maybe-t
            (writer/listen w))]
    (t/is (= #{"world" "Hello"}
             (second (maybe/from-maybe w)))))

  ;; Inner monad values can be lifted into the transformer
  (let [lifted-just (m/with-monad writer-maybe-t
                      (m/lift (maybe/just 3)))
        lifted-nothing (m/with-monad writer-maybe-t
                         (m/lift (maybe/nothing)))]
    (t/is (= (maybe/just (d/pair 3 ["Hello"]))
             (m/with-monad writer-maybe-t
               (m/>> (writer/tell "Hello")
                     lifted-just))))
    (t/is (= (maybe/nothing)
             (m/with-monad writer-maybe-t
               (m/>> (writer/tell "Hello")
                     lifted-nothing))))))
