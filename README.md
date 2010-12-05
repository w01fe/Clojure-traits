Introduction
------------

Protocols, records, and types, new in Clojure 1.2, are great for defining efficient polymorphic functions and corresponding data types.  However, they do not allow implementation inheritance, except in a limited way with the "extend" construct.  

In some cases, it is useful to mix and match implementations of various protocols.  For most use cases, the provided tools are sufficient.  In particular, one can either (1) Have one object that contains objects implementing other protocols, which can be mixed-and-matched, or (2) use extend to mix and match maps of protocol functions.  The downsides are that: in option (1), calling methods "upwards" can be difficult, since the circular references needed must be maintained by hand, and in option (2) we are forced to use (slower) indirect protocol dispatch, and it is not easy to incorporate local state into the protocol implementations.  

This library addresses these issues by adding "traits".  A trait is a bundle of protocol implementations.  It can take arguments, have its own local state, and include other traits.  This is still very much a work in progress; you can see a simple (contrived) example in the src/example.clj file in this repo.  