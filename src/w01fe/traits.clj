(ns edu.berkeley.ai.util.traits
  (:require [edu.berkeley.ai.util :as util]))


(defn- parse-protocols-and-method-pairs [args]
  (when (seq args)
    (let [[proto & rest] args
          [methods more] (split-with coll? rest)]
      (assert (symbol? proto))
      (cons [proto methods]
            (parse-protocols-and-method-pairs more)))))

(defn rewrite-pm-pair [ns args [proto method]]
;  (println ns args proto method)
  (let [method-name    (first method)
        method-args    (second method)
        method-body    (next (next method))
        all-args       (vec (concat method-args args))
        fn-name        (gensym (str proto "-" method-name))
        scoped-fn-name (symbol ns (name fn-name))]
    (assert (apply distinct? (cons nil all-args)))
    [(symbol ns (name proto))
     `(~method-name ~method-args (~scoped-fn-name ~@all-args))
     `(defn ~fn-name ~all-args (loop ~(vec (interleave (next method-args) (next method-args))) ~@method-body))]))
;; Loop allows proper recur semantics ...

(defn- parse-protocols-and-methods [args specs]
  (let [methods-by-proto (parse-protocols-and-method-pairs specs)
        ns               (name (ns-name *ns*))
        pm-pairs         (for [[p ms] methods-by-proto, m ms] [p m])
;        _ (println pm-pairs)
        pm-triples       (map #(rewrite-pm-pair ns args %) pm-pairs)]
    (assert (apply distinct? (cons nil (map first methods-by-proto))))
    [(map #(nth % 2) pm-triples)
     (util/map-vals #(map second %) (group-by first pm-triples))]))

(defn merge-traits [& traits]
  (let [bindings  (vec (apply concat (map first traits)))]
    (assert (apply distinct? (cons nil (take-nth 2 bindings))))
    [bindings
     (reduce util/merge-disjoint {} (map second traits))]))

;; To allow traits to be used from other namespaces, easiest option is to emit named fns in defining ns ?

(defn parse-trait-form [traits]
  (vec (map #(if (list? %)
               (cons (first %) (map (fn [x] `'~x) (rest %)))
               (list %)) traits)))

;; Internal rep. of a trait is a fn from args to [binding-seq impl-map]
;; TODO: forn ow, args may be multiple evaluated?
(defmacro deftrait [name args state-bindings child-traits & protocols-and-methods]
  (let [[method-fn-defs protocol-method-bodies]
        (parse-protocols-and-methods (concat args (take-nth 2 state-bindings)) protocols-and-methods)]
;    (println method-fn-defs "\n" protocol-method-bodies)
    `(do (defn ~name ~args
           (apply merge-traits
                  [(concat (interleave '~args ~args) '~state-bindings)
                   '~protocol-method-bodies]
                  ~(parse-trait-form child-traits)))
         ~@method-fn-defs)))

(defn- render-trait-methods-inline [trait-map]
  (apply concat (map (partial apply cons) trait-map)))

(defmacro reify-traits [[& traits] & specs]
  (let [[trait-bindings trait-methods] (apply merge-traits (eval (parse-trait-form traits)))]
    `(let ~trait-bindings
       (reify
        ~@(render-trait-methods-inline trait-methods)
        ~@specs))))


(do #_comment         

 (defprotocol P2
   (p21 [x y])
   (p22 [x]))


 (defprotocol P1
   (p11 [x y]))

 (defprotocol P0)

 (deftrait +foo+ [x] [y (atom x)] [] P2 (p21 [foo z] (+ z @y)) (p22 [foo] (swap! y inc)) P0)

                                        ; (deftrait +bar+ [w] [z (inc w)] [(+foo+ (* w 2))] P1 (p11 [bar y] (- y w)))
 )

