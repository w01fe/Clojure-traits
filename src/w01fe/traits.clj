(ns w01fe.traits)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Utilities ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn map-vals [f m]
  (into {} (map (fn [[k v]] [k (f v)]) m)))

(defn merge-disjoint [m1 m2]
  (let [ret (merge m1 m2)]
    (assert (= (count ret) (+ (count m1) (count m2))))
    ret))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- parse-protocols-and-method-pairs [args]
  (when (seq args)
    (let [[proto & rest] args
          [methods more] (split-with coll? rest)]
      (assert (symbol? proto))
      (cons [proto methods]
            (parse-protocols-and-method-pairs more)))))

(defn qualify-symbol [s]
  (symbol
   (name (ns-name (if-let [n (namespace s)]
                    (or (find-ns (symbol n))
                        (doto (get (ns-aliases *ns*) (symbol n)) assert))
                   *ns*)))
   (name s)))


(defn rewrite-pm-pair [trait-name args [proto method]]
  (let [method-name    (first method)
        method-args    (second method)
        method-body    (next (next method))
        all-args       (vec (concat method-args args))
        fn-name        (gensym (str trait-name "-" (name proto) "-" method-name))
        scoped-fn-name (symbol (name (ns-name *ns*)) (name fn-name))]
    (assert (apply distinct? (cons nil all-args)))
    [(qualify-symbol proto)
     `(~method-name ~method-args (~scoped-fn-name ~@all-args))
     `(defn ~fn-name ~all-args (loop ~(vec (interleave (next method-args) (next method-args))) ~@method-body))]))
;; Loop allows proper recur semantics ...

(defn- parse-protocols-and-methods [trait-name args specs]
  (let [methods-by-proto (parse-protocols-and-method-pairs specs)
        pm-pairs         (for [[p ms] methods-by-proto, m ms] [p m])
;        _ (println pm-pairs)
        pm-triples       (map #(rewrite-pm-pair trait-name args %) pm-pairs)]
    (assert (apply distinct? (cons nil (map first methods-by-proto))))
    [(doall (map #(nth % 2) pm-triples))
     (map-vals #(map second %) (group-by first pm-triples))]))

(defn merge-traits [& traits]
  (let [bindings  (vec (apply concat (map first traits)))]
    (assert (apply distinct? (cons nil (take-nth 2 bindings))))
    [bindings
     (reduce merge-disjoint {} (map second traits))]))

;; To allow traits to be used from other namespaces, easiest option is to emit named fns in defining ns ?
(defn parse-trait-form [traits]
  (vec (map #(if (list? %)
               (cons (first %) (map (fn [x] `'~x) (rest %)))
               (list %)) traits)))

(defn- render-trait-methods-inline [trait-map]
  (apply concat (map (partial apply cons) trait-map)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;; Public Interface ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Internal rep. of a trait is a fn from args to [binding-seq impl-map]
;; TODO: for now, args may be multiple evaluated?
(defmacro deftrait
  "Define a new trait with the given name. Traits are essentially bundles of
   protocol implementaitons, which can include arguments, local state, and
   inheritance.  Objects using the defined trait(s) are created using
   reify-traits.

   args is a list of trait argument bindings, like a defn arglist.
   state-bindings is a set of further bindings, like a 'let', which
     can create local state via reference types, and refer to the args.
   child-traits is a vector of child traits, which are (name & args)
     calls, or bare names for no-arg traits.
   protocols-and-methods are a set of protocols and methods to implement,
     as in defrecord or reify, which can refer to args and state-bindings."

  [name args state-bindings child-traits & protocols-and-methods]
  (let [[method-fn-defs protocol-method-bodies]
        (parse-protocols-and-methods name (concat args (take-nth 2 state-bindings)) protocols-and-methods)]
    `(do (defn ~name ~args
           (apply merge-traits
                  [(concat (interleave '~args ~args) '~state-bindings)
                   '~protocol-method-bodies]
                  ~(parse-trait-form child-traits)))
         ~@method-fn-defs)))

(defmacro reify-traits
  "Reify a new object that includes the named traits, plus (optional)
   additional protocols and method implementations as in ordinary reify.
   Traits can be raw names (for 0-arg traits), or (name & args) calls."
  [[& traits] & specs]
  (let [[trait-bindings trait-methods] (apply merge-traits (eval (parse-trait-form traits)))]
    `(let ~trait-bindings
       (reify
        ~@(render-trait-methods-inline trait-methods)
        ~@specs))))



