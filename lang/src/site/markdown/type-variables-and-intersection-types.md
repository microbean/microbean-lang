# Type Variables and Intersection Types and Interfaces

An `IntersectionType` is just a way of representing the many bounds of a `TypeVariable` in a single type. This also
means that `IntersectionType`s must play by the many rules imposed by `TypeVariable`s, the only types that "use" them.

All of an `IntersectionType`'s bounds will be `DeclaredType`s. Notably, none of them will be `ArrayType`s,
`IntersectionType`s, or `TypeVariable`s. Only its first may be a `DeclaredType` declared by a non-interface
`TypeElement`.

In the Java language model, a `TypeVariable` has exactly one upper bound, which may be an `IntersectionType`.

If its sole bound is not an `IntersectionType`, then it will be either a `DeclaredType`, or a `TypeVariable`. It will
never be an `ArrayType`.

We can tell if a `DeclaredType` "is an interface" by seeing if its declaring `TypeElement` is an interface. (Strictly
speaking a `DeclaredType` is not itself an interface; only its declaring `TypeElement` can be an interface.)

We can tell if an `IntersectionType` "is an interface" by seeing if its first bound, a `DeclaredType`, "is an interface"
by the rule above. (Strictly speaking, an `IntersectionType` cannot itself be an interface, of course; we're checking to
see if all of its bounds are interfaces or not, and we can do this by simply checking the first of its bounds.)

Applying these rules together and recursively where necessary, we can therefore tell whether any given `TypeVariable`
"is an interface" if either (a) its sole bound is a `DeclaredType` or `TypeVariable` that "is an interface" or (b) its
sole bound is an `IntersectionType` whose first bound is a `DeclaredType` that "is an interface". (Strictly speaking, a
`TypeVariable` cannot itself be an interface, of course; we're checking to see if its bound "is an interface" according
to these rules.)
