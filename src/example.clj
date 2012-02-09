(ns example
  (:require [w01fe.traits :as traits]))

(defprotocol Counter
  (inc! [c x] "Increment count and return old value"))

(traits/deftrait SimpleCounter [inc] [count-atom (atom inc)] []
  Counter
  (inc! [c x] (let [old @count-atom] (swap! count-atom + x) old)))

(defprotocol Multiplier
  (mul! [c x] "Multiply count and return old value"))

(traits/deftrait SimpleMultiplier [mul] [mul-atom (atom mul)] []
  Multiplier
  (mul! [c x] (let [old @mul-atom] (swap! mul-atom * x) old)))

(traits/deftrait LinearCounter [start] [] [( SimpleCounter start) ( SimpleMultiplier start)])



;;user> (def x (reify-traits [( LinearCounter 5)]))
;;#'user/x
;;user> (inc! x 10)
;;5
;;user> (inc! x 10)
;;15
;;user> (mul! x 10)
;;5
;;user> (mul! x 10)
;;50
;;user> (mul! x 10)
;;500
;;user> (inc! x 10)
;;25
